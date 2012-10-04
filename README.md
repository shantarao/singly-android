# Singly Android Client

## Alpha
Be aware that the Singly Android Client is in an alpha state.  Package names, class names, and method names are subject to change.  As we work to make the sdk better we are not concerned with breaking backwards compatibility for now.  This will change in the future as the sdk becomes more stable.

## Overview
This repository contains two different Android projects.  The first is the Singly Android client library.  This is a library project you can include into your Android apps that makes it easy to use the Singly API.  The second in an example Android project that show useage of the Singly client.

The Singly Android client is a library supporting the [Singly](https://singly.com) social API that will let you

  - Allow users to easily authenticate with any service supported by Singly; for example Facebook, Twitter, Github, Foursquare and others
  - Make requests to the Singly API to retrieve your users' social data for use in your app


The library code is contained in the SinglyAndroidSDK project in the sdk folder.  The com.singly.android.client.SinglyClient class is the entry point to using the Singly API in your Android project.

Sample implementations are contained in the SinglyAndroidExamples project in the examples.  The com.singly.android.examples.MainActivity and ProfilesActivity show usage of the SinglyClient class to authenticate and perform API calls.

## Registering Your Singly App

In order to use the Android client you will need to do the following:

1. Go to https://singly.com and register or login.
2. Create your app or use the default app.
3. Get the client id and client secret for your app.  
4. If using the examples, copy and past the client id and client secret into the SinglyClient constructor at the top of the MainActivity and ProfilesActivity classes in the Singly Android Example project.
5. If creating your own Andorid app, use the client id and client secret in the SinglyClient constructor.


## Using the Android Client

### AuthenticationActivity
To use the SinglyClient you will need to register the com.singly.android.client.AuthenticationActivity class as an activity in your AndroidManifest.xml file.  It will look like this:

    <activity android:name="com.singly.android.client.AuthenticationActivity" />

### Instantiating the SinglyClient
Next your will need to create the SinglyClient class as shown below.  Follow the Singly app registration process described above to obtain your client id and client secret. 

    SinglyClient api = new SinglyClient("your_client_id", "your_client_secret");

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

Support
--------------

This is a work in progress. If you have questions or comments

* Join our live chatroom at http://support.singly.com
* Email support@singly.com
