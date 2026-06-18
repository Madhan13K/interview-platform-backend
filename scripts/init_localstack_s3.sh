#!/bin/bash
# Script to create the S3 bucket in LocalStack for local development
# Run this after docker-compose up

echo "Waiting for LocalStack to be ready..."
until curl -s http://localhost:4566/_localstack/health | grep -q '"s3": "available"'; do
    sleep 2
done

echo "Creating S3 bucket: interview-platform-documents"
aws --endpoint-url=http://localhost:4566 s3 mb s3://interview-platform-documents --region us-east-1 2>/dev/null || true

echo "Verifying bucket creation..."
aws --endpoint-url=http://localhost:4566 s3 ls

echo "Done! S3 bucket is ready for local development."
echo ""
echo "Use these environment variables for local dev:"
echo "  AWS_S3_BUCKET_NAME=interview-platform-documents"
echo "  AWS_S3_REGION=us-east-1"
echo "  AWS_S3_ACCESS_KEY=test"
echo "  AWS_S3_SECRET_KEY=test"
echo "  AWS_S3_ENDPOINT=http://localhost:4566"

