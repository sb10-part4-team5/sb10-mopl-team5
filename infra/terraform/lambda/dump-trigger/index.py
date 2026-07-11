import json
import os
import time
import urllib.parse
import urllib.request
from base64 import b64encode

import boto3

ecs = boto3.client("ecs")
ssm = boto3.client("ssm")

CLUSTER = os.environ["ECS_CLUSTER"]
SERVICE = os.environ["ECS_SERVICE"]

LOKI_URL = os.environ["LOKI_URL"]
LOKI_USERNAME = os.environ["LOKI_USERNAME"]
LOKI_READ_TOKEN = os.environ["LOKI_READ_TOKEN"]
LOKI_QUERY = '{job="mopl"} |= "Thread starvation"'

# 폴링 주기(1분)보다 살짝 넓게 겹쳐서 조회해 인제스트 지연으로 놓치는 걸 방지한다.
LOOKBACK_SECONDS = 90

COOLDOWN_PARAM = "/mopl/dump-trigger/last-triggered-at"
COOLDOWN_SECONDS = 300

DUMP_SCRIPT = (
    'CID=$(docker ps --filter "label=com.amazonaws.ecs.container-name=mopl" '
    '--format "{{.ID}}" | head -1); '
    'if [ -z "$CID" ]; then echo "mopl container not found"; exit 1; fi; '
    'docker exec "$CID" kill -3 1; '
    "sleep 2; "
    'docker exec "$CID" sh -c "jcmd 1 GC.heap_info >> /proc/1/fd/1" || true'
)


def handler(event, context):
    if not thread_starvation_detected():
        return {"triggered": False, "reason": "no matching logs"}

    if in_cooldown():
        return {"triggered": False, "reason": "cooldown active"}

    instance_id = find_running_instance()
    if not instance_id:
        return {"triggered": False, "reason": "no running mopl task found"}

    command_id = trigger_dump(instance_id)
    mark_triggered()
    return {"triggered": True, "instanceId": instance_id, "commandId": command_id}


def thread_starvation_detected():
    now_ns = time.time_ns()
    start_ns = now_ns - LOOKBACK_SECONDS * 1_000_000_000

    query = urllib.parse.urlencode(
        {"query": LOKI_QUERY, "start": start_ns, "end": now_ns, "limit": 1}
    )
    url = f"{LOKI_URL}/loki/api/v1/query_range?{query}"

    credentials = b64encode(f"{LOKI_USERNAME}:{LOKI_READ_TOKEN}".encode()).decode()
    request = urllib.request.Request(
        url, headers={"Authorization": f"Basic {credentials}"}
    )

    with urllib.request.urlopen(request, timeout=10) as response:
        body = json.loads(response.read())

    results = body.get("data", {}).get("result", [])
    return len(results) > 0


def in_cooldown():
    try:
        param = ssm.get_parameter(Name=COOLDOWN_PARAM)
        last_triggered = int(param["Parameter"]["Value"])
    except ssm.exceptions.ParameterNotFound:
        return False

    return (time.time() - last_triggered) < COOLDOWN_SECONDS


def mark_triggered():
    ssm.put_parameter(
        Name=COOLDOWN_PARAM,
        Value=str(int(time.time())),
        Type="String",
        Overwrite=True,
    )


def find_running_instance():
    task_arns = ecs.list_tasks(
        cluster=CLUSTER, serviceName=SERVICE, desiredStatus="RUNNING"
    )["taskArns"]
    if not task_arns:
        return None

    tasks = ecs.describe_tasks(cluster=CLUSTER, tasks=task_arns)["tasks"]
    if not tasks:
        return None

    container_instance_arn = tasks[0]["containerInstanceArn"]
    container_instances = ecs.describe_container_instances(
        cluster=CLUSTER, containerInstances=[container_instance_arn]
    )["containerInstances"]
    if not container_instances:
        return None

    return container_instances[0]["ec2InstanceId"]


def trigger_dump(instance_id):
    response = ssm.send_command(
        InstanceIds=[instance_id],
        DocumentName="AWS-RunShellScript",
        Parameters={"commands": [DUMP_SCRIPT]},
        TimeoutSeconds=60,
    )
    return response["Command"]["CommandId"]
