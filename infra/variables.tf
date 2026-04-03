variable "ghcr_token" {
  type        = string
  sensitive   = true
  description = "GitHub PAT for docker login ghcr.io (e.g. write:packages)."
}
