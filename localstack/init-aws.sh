#!/bin/bash
set -e

echo "Initializing LocalStack AWS resources for TruckDar Notifications..."

# Create S3 Bucket for voice audio
awslocal s3 mb s3://truckdar-voice-notifications || true

# Create SNS Topic for push notifications
awslocal sns create-topic --name truckdar-notifications || true

echo "LocalStack resources initialized successfully."
