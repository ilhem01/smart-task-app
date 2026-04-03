output "instance_public_ip" {
  description = "Public IPv4 for SSH and app access (use this host in ssh/ec2-user@...)."
  value       = aws_instance.app.public_ip
}

output "ssh_command" {
  description = "Example SSH using the configured key (adjust path to your .pem)."
  value       = "ssh -i smart-key-new.pem ec2-user@${aws_instance.app.public_ip}"
}
