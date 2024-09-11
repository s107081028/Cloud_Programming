package com.example.cloudprog.ui;
//lab9-2 import
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.GroupGrantee;
import com.amazonaws.services.s3.model.Permission;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import android.os.StrictMode;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SetQueueAttributesRequest;
import com.example.cloudprog.R;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Principal;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.Statement.Effect;
import com.amazonaws.auth.policy.actions.SQSActions;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //network policy
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Initialize the Amazon Cognito credentials provider
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-east-2:56e8b4e9-1f84-4af6-8be1-7b66b3eec894", // Use your Identity pool ID
                Regions.US_EAST_2 // Region
        );
        // Initialize s3Client
        final AmazonS3Client s3Client = new AmazonS3Client(credentialsProvider.getCredentials());

        Button button1 = findViewById(R.id.camera_btn);
        button1.setOnClickListener(btn_1_click);

        Button button2 = findViewById(R.id.create_bucket_btn);
        button2.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //Todo : create a s3 bucket & add permission
                try{
                    //Check bucket name in app/res/values/string.xml
                    AccessControlList acl = new AccessControlList();
                    acl.grantPermission(GroupGrantee.AllUsers, Permission.FullControl);
//                    CreateBucketRequest createBucketRequest = new CreateBucketRequest(getString(R.string.bucket_name))
//                            .withAccessControlList(acl).withCannedAcl(CannedAccessControlList.PublicReadWrite);
//                    s3Client.createBucket(createBucketRequest);
                    s3Client.createBucket(getString(R.string.bucket_name));
                    Toast.makeText(MainActivity.this, "Create success", Toast.LENGTH_LONG).show();
                }catch(Exception e){
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Create fail", Toast.LENGTH_LONG).show();
                }
            }
        });

        Button button3 = findViewById(R.id.delete_bucket_btn);
        button3.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //Todo : delete a s3 bucket
                try{
                    //Check bucket name in app/res/values/string.xml
                    ObjectListing objectListing = s3Client.listObjects(getString(R.string.bucket_name));
                    while (true) {
                        for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                            s3Client.deleteObject(new DeleteObjectRequest(getString(R.string.bucket_name), objectSummary.getKey()));
                        }
                        if (objectListing.isTruncated()) {
                            objectListing = s3Client.listNextBatchOfObjects(objectListing);
                        } else {
                            break;
                        }
                    }
                    s3Client.deleteBucket(getString(R.string.bucket_name));
                    Toast.makeText(MainActivity.this, "Delete success", Toast.LENGTH_LONG).show();
                }catch(Exception e){
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Delete fail", Toast.LENGTH_LONG).show();
                }
            }
        });

        final AmazonSQSClient sqsClient = new AmazonSQSClient(credentialsProvider.getCredentials());
//        sqsClient.setEndpoint("https://sqs." + "us-east-2" + ".amazonaws.com");
        Button button4 = findViewById(R.id.create_queue_btn);
        button4.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {//Todo : create a sqs queue & add permission
                try{
                    CreateQueueRequest createQueueRequest = new CreateQueueRequest().withQueueName(getString(R.string.queue_name));
//                    CreateQueueResult result = sqsClient.createQueue(getString(R.string.queue_name));
                    CreateQueueResult result = sqsClient.createQueue(createQueueRequest);
                    GetQueueAttributesRequest qRequest = new GetQueueAttributesRequest().withQueueUrl(result.getQueueUrl())
                            .withAttributeNames("QueueArn");
                    GetQueueAttributesResult qResult = sqsClient.getQueueAttributes(qRequest);
                    Policy sqsPolicy = new Policy().withStatements(new Statement(Effect.Allow).withPrincipals(Principal.AllUsers)
                            .withActions(SQSActions.SendMessage).withResources(new Resource(qResult.getAttributes().get("QueueArn"))));
                    Map<String, String> queueAttributes = new HashMap<String, String>();
                    queueAttributes.put("Policy", sqsPolicy.toJson());
                    sqsClient.setQueueAttributes(new SetQueueAttributesRequest(result.getQueueUrl(), queueAttributes));
                    Toast.makeText(MainActivity.this, "Create success", Toast.LENGTH_LONG).show();
                }catch(Exception e){
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Create fail", Toast.LENGTH_LONG).show();
                }
            }
        });

        Button button5 = findViewById(R.id.delete_queue_btn);
        button5.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //Todo : delete a sqs queue
                try {
                    String queueUrl = sqsClient.getQueueUrl(getString(R.string.queue_name)).getQueueUrl();
                    ReceiveMessageRequest request = new ReceiveMessageRequest(queueUrl);
                    ReceiveMessageResult result = sqsClient.receiveMessage(request);

                    while (!result.getMessages().isEmpty()) {
                        for (Message message : result.getMessages()) {
                            sqsClient.deleteMessage(queueUrl, message.getReceiptHandle());
                        }

                        result = sqsClient.receiveMessage(request);
                    }
                    sqsClient.deleteQueue(queueUrl);
                    Toast.makeText(MainActivity.this, "Delete success", Toast.LENGTH_LONG).show();
                }catch(Exception e){
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Delete fail", Toast.LENGTH_LONG).show();
                }
            }
        });

    }
    private View.OnClickListener btn_1_click = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(MainActivity.this, CameraActivity.class);
            startActivity(intent);
        }
    };
}
