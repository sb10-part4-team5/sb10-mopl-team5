# mopl 전용 VPC
resource "aws_vpc" "mopl" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = { Name = "mopl-vpc" }
}

# 인터넷 출입구
resource "aws_internet_gateway" "mopl" {
  vpc_id = aws_vpc.mopl.id

  tags = { Name = "mopl-igw" }
}

# 사용 가능한 가용영역 조회
data "aws_availability_zones" "available" {
  state = "available"
}

# 퍼블릭 서브넷 2개 (서로 다른 AZ — RDS가 2개 AZ를 요구)
resource "aws_subnet" "public" {
  count = 2

  vpc_id                  = aws_vpc.mopl.id
  cidr_block              = "10.0.${count.index + 1}.0/24"
  availability_zone       = data.aws_availability_zones.available.names[count.index]
  map_public_ip_on_launch = true

  tags = { Name = "mopl-public-${count.index + 1}" }
}

# 퍼블릭 라우팅: 모든 외부 트래픽을 인터넷 게이트웨이로
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.mopl.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.mopl.id
  }

  tags = { Name = "mopl-public-rt" }
}

resource "aws_route_table_association" "public" {
  count = 2

  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

# 프라이빗 서브넷 2개 (DB 전용 — 인터넷 경로 없음, 2개 AZ)
resource "aws_subnet" "private" {
  count = 2

  vpc_id            = aws_vpc.mopl.id
  cidr_block        = "10.0.${count.index + 11}.0/24"
  availability_zone = data.aws_availability_zones.available.names[count.index]

  tags = { Name = "mopl-private-${count.index + 1}" }
}

# 프라이빗 라우팅: VPC 내부(로컬)만 — IGW 라우트 없음 → 인터넷 격리, NAT 불필요
resource "aws_route_table" "private" {
  vpc_id = aws_vpc.mopl.id

  tags = { Name = "mopl-private-rt" }
}

resource "aws_route_table_association" "private" {
  count = 2

  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private.id
}
