# Singly Android SDK

## Alpha
Be aware that the Singly Android Client is in an alpha state.  Package names, class names, and method names are subject to change.  As we work to make the sdk better we are not concerned with breaking backwards compatibility for now.  This will change in the future as the sdk becomes more stable.

## Overview
This repository contains two different Android projects.  The first is the Singly Android client library.  This is a library project you can include into your Android apps that makes it easy to use the Singly API.  The second in an example Android project that show useage of the Singly client.

The Singly Android client is a library supporting the [Singly](https://singly.com) social API that will:

  - Allow users to easily authenticate with any service supported by Singly; for example Facebook, Twitter, Github, Foursquare and others
  - Make requests to the Singly API to retrieve your users' social data for use in your app


The library code is contained in the SinglyAndroidSDK project in the sdk folder.  The com.singly.android.client.SinglyClient class is the entry point to using the Singly API in your Android project.

Sample implementations are contained in the SinglyAndroidExamples project in the examples.  The com.singly.android.examples.MainActivity and ProfilesActivity show usage of the SinglyClient class to authenticate and perform API calls.

## Using the Singly Android SDK

The flow of using the SDK is as follows

1. Go to https://singly.com and register or login.
2. Create your app or use the default app.
3. Get the client id and client secret for your app.
4. Set your client id and client secret in the SinglyClient private constructor.  This is in the source code of the Singly Android SDK library.
5. Register the AuthenticationActivity in your AndroidManifest.xml file.
6. Get an instance of the SinglyClient using the SinglyClient.getInstance() method.
7. Authenticate a user against one or more services that singly supports.  This gives you a singly access token.
8. Call Singly API methods.

## Using the Android Client

### Setting Your Singly Keys
You will also need to setup your Singly app client id and client secret.  This is set in the source code of the Singly Android SDK library.  Go to the com.singly.android.client.SinglyClient class and in the private constructor you will see two lines like this:

    this.clientId = "your_client_id";
    this.clientSecret = "your_client_secret";

You will need to change those two lines to be your client id and client secret.  Follow the Singly app registration process described above to obtain your client id and client secret. 

### AuthenticationActivity
To use the SinglyClient you will need to register the com.singly.android.client.AuthenticationActivity class as an activity in your AndroidManifest.xml file.  It will look like this:

    <activity android:name="com.singly.android.client.AuthenticationActivity" />

### Instantiating the SinglyClient
Once you have set your client id and client secret you can obtain an instance of the SinglyClient using the getInstance method. 

    SinglyClient api = SinglyClient.getInstance();

The SinglyClient is a singleton.  Only one instance of the client is used throughout an Android application.

### Authenticate the User
To access the Singly API your user must first authenticate with one or more services that Singly supports.  This is done through the `authenticate()` method of the SinglyClient class.  The authenticate method will launch an AuthenticationActivity that handles authenticating the user with the service.  The current Android context and a service name to authenticate against are passed in.  You can replace the service name "facebook" with [any service Singly supports](https://singly.com/docs)

    api.authenticate(context, "facebook");

### Retrieve User Social Data
Once a user has authenticated with a service you will be able to make api calls to the Singly API to retrieve the user's social data.  This is done throught the `apiCall` method of the `SinglyClient` class.  To make an api call you provide the api path and any api parameters.  Access token is not required as it will be appended to any api calls made through the client.  All api calls are performed asynchronously.  An `APICallListener` class is provided to callback on the success or error of the call.  Upon success the JSONObject reprsenting the api response is returned to the listener.  This data can then be used within your Android app.
    
    api.doGetApiRequest(context, "/profiles", queryParams, new AsyncApiResponseHandler() {

      public void onSuccess(String response) {
        // the response from the api, usually a JSON string, is passed in
      }

      public void onFailure(Throwable error) {
        // error performing the api request
      }
    });

For more information on available Singly api calls check out our [API Overview](https://singly.com/docs/api).

## Java SDK

If you are building a Java desktop or web application, not an Android app, it is better to use the Singly SDK for Java.

https://github.com/Singly/singly-java

Support
--------------

This is a work in progress. If you have questions or comments

* Join our live chatroom at http://support.singly.com
* Email support@singly.com
