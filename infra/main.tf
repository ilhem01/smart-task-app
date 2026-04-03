resource "aws_security_group" "app_sg" {
  name = "app-security-group"

  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "App Port"
    from_port   = 3000
    to_port     = 3000
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "auth-service host"
    from_port   = 4001
    to_port     = 4001
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "task-service host"
    from_port   = 4002
    to_port     = 4002
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "api-gateway host"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_instance" "app" {
  ami           = "ami-0e872aee57663ae2d"
  instance_type = "t2.micro"

  vpc_security_group_ids = [aws_security_group.app_sg.id]

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
