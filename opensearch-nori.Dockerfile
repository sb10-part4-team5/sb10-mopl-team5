# 로컬 개발용 OpenSearch 이미지 (docker-compose의 opensearch 서비스가 사용).
#
# 한국어 형태소 분석기(nori)는 OpenSearch 기본 이미지에 포함되어 있지 않아 직접 설치한다.
# 콘텐츠 제목/설명 검색이 한국어라 nori 없이는 검색 품질이 떨어진다.
#
# 운영(AWS OpenSearch Service)에서는 이 파일을 쓰지 않는다. 거기서는 nori를 ZIP-PLUGIN 패키지로
# 도메인에 associate 하는 방식이다.
#
# 버전은 AWS가 지원하는 최신 버전(3.5)에 맞춘다. 플러그인 버전은 OpenSearch 버전과 정확히
# 일치해야 하므로 태그를 올릴 때 nori 설치가 깨지지 않는지 같이 확인할 것.
FROM opensearchproject/opensearch:3.5.0

RUN /usr/share/opensearch/bin/opensearch-plugin install --batch analysis-nori
