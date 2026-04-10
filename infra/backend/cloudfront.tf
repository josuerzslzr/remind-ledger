# ---------------------------------------------------------------------------
# CloudFront distribution in front of the ALB
# Provides trusted HTTPS via the default *.cloudfront.net certificate ($0).
# Origin traffic flows over HTTP; access is restricted to this distribution
# via a shared secret header (X-Origin-Verify).
# ---------------------------------------------------------------------------

resource "random_uuid" "origin_verify" {}

resource "aws_cloudfront_distribution" "api" {
  enabled         = true
  comment         = "${var.project} API (ALB origin)"
  http_version    = "http2and3"
  is_ipv6_enabled = true

  origin {
    domain_name = aws_lb.main.dns_name
    origin_id   = "alb"

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "http-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }

    custom_header {
      name  = "X-Origin-Verify"
      value = random_uuid.origin_verify.result
    }
  }

  default_cache_behavior {
    target_origin_id       = "alb"
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods         = ["GET", "HEAD"]

    # Managed policy: CachingDisabled
    cache_policy_id = "4135ea2d-6df8-44a3-9df3-4b5a84be39ad"

    # Managed policy: AllViewerExceptHostHeader
    origin_request_policy_id = "b689b0a8-53d0-40ab-baf2-68738e2966ac"
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    cloudfront_default_certificate = true
  }

  tags = {
    Name = "${var.project}-api-cdn"
  }
}
