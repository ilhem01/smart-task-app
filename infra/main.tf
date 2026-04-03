locals {
  app_ingress_ports = {
    80   = "HTTP"
    3000 = "frontend"
    4001 = "auth-service"
    4002 = "task-service"
    8080 = "api-gateway"
  }
}

# -----------------------------
# Security Group
# -----------------------------
resource "aws_security_group" "app_sg" {
  name                   = "app-security-group"
  revoke_rules_on_delete = true

  # SSH (optional, but kept)
  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # App ports
  dynamic "ingress" {
    for_each = local.app_ingress_ports
    content {
      description = ingress.value
      from_port   = ingress.key
      to_port     = ingress.key
      protocol    = "tcp"
      cidr_blocks = ["0.0.0.0/0"]
    }
  }

  # Outbound
  egress {
    description = "All outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# -----------------------------
# IAM Role for SSM
# -----------------------------
resource "aws_iam_role" "ec2_role" {
  name = "ec2-ssm-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [{
      Action = "sts:AssumeRole",
      Effect = "Allow",
      Principal = {
        Service = "ec2.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ssm_attach" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "ec2_profile" {
  name = "ec2-profile"
  role = aws_iam_role.ec2_role.name
}

# -----------------------------
# EC2 Instance
# -----------------------------
resource "aws_instance" "app" {
  ami                         = "ami-0e872aee57663ae2d"
  instance_type               = "t2.micro"
  key_name                    = "final-key"
  associate_public_ip_address = true

  vpc_security_group_ids = [aws_security_group.app_sg.id]
  iam_instance_profile   = aws_iam_instance_profile.ec2_profile.name

  user_data = <<-EOF
#!/bin/bash
yum update -y

# Install Docker
yum install docker -y
systemctl start docker
systemctl enable docker
usermod -aG docker ec2-user

# Install Docker Compose
curl -L "https://github.com/docker/compose/releases/download/v2.20.2/docker-compose-linux-x86_64" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# Login to GHCR
echo "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" | docker login ghcr.io -u ilhem01 --password-stdin

# Create app directory
mkdir -p /home/ec2-user/app
cd /home/ec2-user/app

# Create docker-compose.yml
cat <<EOT > docker-compose.yml
version: '3'
services:

  auth-service:
    image: ghcr.io/ilhem01/smart-task-auth-service:latest
    ports:
      - "4001:4001"

  task-service:
    image: ghcr.io/ilhem01/smart-task-task-service:latest
    ports:
      - "4002:4002"

  api-gateway:
    image: ghcr.io/ilhem01/smart-task-api-gateway:latest
    ports:
      - "8080:8080"

  frontend:
    image: ghcr.io/ilhem01/smart-task-frontend:latest
    ports:
      - "3000:3000"

EOT

# Run containers
docker-compose up -d
EOF

  tags = {
    Name = "smart-task-app"
  }
}