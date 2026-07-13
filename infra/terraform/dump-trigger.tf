# 헬스체크 실패 조짐(HikariCP thread starvation 등)을 감지하면, 현재 떠있는 mopl 태스크의
# EC2에 SSM으로 원격 명령을 보내 스레드 덤프(kill -3)와 힙 요약(jcmd GC.heap_info)을 뜬다.
# 결과는 컨테이너 stdout으로 찍히고 Alloy가 그대로 Loki까지 실어 나른다.
#
# 이 계정은 Lambda Function URL의 공개(NONE) 인증이 리소스 정책상 허용돼 있어도 403으로
# 막히는 제약이 있어(낮은 ConcurrentExecutions 한도로 볼 때 샌드박스성 계정으로 추정),
# Grafana 웹훅으로 외부에서 호출받는 대신 EventBridge Scheduler가 1분마다 Lambda를 깨워
# Loki를 직접 폴링하는 방식으로 구성한다. 공개 엔드포인트가 아예 없어 그 제약 자체를 우회한다.

data "archive_file" "dump_trigger_lambda" {
  type        = "zip"
  source_dir  = "${path.module}/lambda/dump-trigger"
  output_path = "${path.module}/lambda/dump-trigger.zip"
}

resource "aws_iam_role" "dump_trigger_lambda" {
  name = "mopl-dump-trigger-lambda-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "lambda.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "dump_trigger_lambda_basic" {
  role       = aws_iam_role.dump_trigger_lambda.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_iam_role_policy" "dump_trigger_lambda" {
  name = "mopl-dump-trigger-lambda-policy"
  role = aws_iam_role.dump_trigger_lambda.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ecs:ListTasks",
          "ecs:DescribeTasks",
          "ecs:DescribeContainerInstances",
        ]
        Resource = "*"
        Condition = {
          ArnEquals = {
            "ecs:cluster" = aws_ecs_cluster.mopl.arn
          }
        }
      },
      {
        Effect   = "Allow"
        Action   = "ssm:SendCommand"
        Resource = "arn:aws:ssm:${var.aws_region}::document/AWS-RunShellScript"
      },
      {
        Effect   = "Allow"
        Action   = "ssm:SendCommand"
        Resource = "arn:aws:ec2:${var.aws_region}:${var.account_id}:instance/*"
        Condition = {
          StringEquals = {
            "ssm:resourceTag/AmazonECSManaged" = "true"
          }
        }
      },
      {
        Effect   = "Allow"
        Action   = ["ssm:GetParameter", "ssm:PutParameter"]
        Resource = "arn:aws:ssm:${var.aws_region}:${var.account_id}:parameter/mopl/dump-trigger/*"
      },
    ]
  })
}

resource "aws_lambda_function" "dump_trigger" {
  function_name = "mopl-dump-trigger"
  role          = aws_iam_role.dump_trigger_lambda.arn
  handler       = "index.handler"
  runtime       = "python3.12"
  timeout       = 20

  filename         = data.archive_file.dump_trigger_lambda.output_path
  source_code_hash = data.archive_file.dump_trigger_lambda.output_base64sha256

  environment {
    variables = {
      ECS_CLUSTER     = aws_ecs_cluster.mopl.name
      ECS_SERVICE     = var.ecs_service
      LOKI_URL        = var.grafana_loki_url
      LOKI_USERNAME   = var.grafana_loki_username
      LOKI_READ_TOKEN = var.grafana_loki_read_token
    }
  }
}

resource "aws_cloudwatch_log_group" "dump_trigger_lambda" {
  name              = "/aws/lambda/${aws_lambda_function.dump_trigger.function_name}"
  retention_in_days = 14
}

# EventBridge Scheduler가 1분마다 Lambda를 직접 호출 (공개 엔드포인트 없음)
resource "aws_iam_role" "dump_trigger_scheduler" {
  name = "mopl-dump-trigger-scheduler-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "scheduler.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy" "dump_trigger_scheduler" {
  name = "mopl-dump-trigger-scheduler-invoke"
  role = aws_iam_role.dump_trigger_scheduler.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = "lambda:InvokeFunction"
      Resource = aws_lambda_function.dump_trigger.arn
    }]
  })
}

resource "aws_scheduler_schedule" "dump_trigger_poll" {
  name                         = "mopl-dump-trigger-poll"
  schedule_expression          = "rate(1 minute)"
  schedule_expression_timezone = "Asia/Seoul"

  flexible_time_window {
    mode = "OFF"
  }

  target {
    arn      = aws_lambda_function.dump_trigger.arn
    role_arn = aws_iam_role.dump_trigger_scheduler.arn
  }
}
