import boto3
import urllib3
from PIL import Image
from io import BytesIO
import io



def lambda_handler(event, context):
    # # Connect to SQS queue
    # sqs = boto3.resource('sqs')
    # queue = sqs.get_queue_by_name(QueueName='my-queue')
    
    
    # # # Get message from SQS queue
    # messages = queue.receive_messages(MaxNumberOfMessages=1)
    # # if len(messages) == 0:
    # #     return
    # message = messages[0]
    # # image_url = message.body
    # # # image_url = message.MessageBody

    image_url = event['Records'][0]['body']
    
    print("This is img url: ",image_url)
    # Download image
    http = urllib3.PoolManager()
    response = http.request('GET', image_url)
    # image_data = response.data
    
    # # Resize image


    # Load the image data into a PIL Image object
    img = Image.open(BytesIO(response.data))
    print(type(img))
    (w, h) = img.size
    # print('w=%d, h=%d', w, h)
    
    # Resize the image using PIL
    img.resize((int(w/2), int(h/2)))
    
    # Save the resized image data to a stream using the JPEG format
    # stream = BytesIO()
    
    # print("this is stream: ",stream)
    # resized_img.save(stream, format='png')
    # img.save(stream,f"{stream}.png")
    
    # resized_img.save(f"{stream}.jpg")
    # img.save("testimg.jpg")
    
    
    # Save the image to an in-memory file
    stream = io.BytesIO()
    img.save(stream, format=img.format)
    stream.seek(0)
    
    
    

    
    # # Connect to S3 bucket
    # s3 = boto3.resource('s3')
    # bucket = s3.Bucket('nthu-109020015-resized')
    
    # # Upload resized image to S3
    # bucket.put_object(Key='resized.jpg', Body=resized_img)

    # print("Done!")
    
    # Delete message from SQS queue
    # message.delete()
    # Upload the resized image data to S3
    s3 = boto3.client('s3')
    
    bucket_name = 'cloudprog-hw3-110501707'
    # s3_key = 'resized/' + image_url.split('/')[-1]
    s3_key = "resized" + image_url.split('/')[-1]
    
    s3.upload_fileobj(
        stream, # This is what i am trying to upload
        bucket_name,
        s3_key,
        ExtraArgs={
            'ACL': 'public-read'
        }
    )
    
    
    # s3.upload_fileobj(img, bucket_name, s3_key)
    
    return {
        'statusCode': 200,
        'body': 'Image resized and uploaded to S3 successfully'
    }
