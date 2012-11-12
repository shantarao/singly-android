# Singly Android SDK

## Alpha State
Be aware that the Singly Android Client is in an alpha state.  Package names, class names, and method names are subject to change.  As we work to evolve the sdk we are not as concerned with breaking backwards compatibility.  This will change in the future as the sdk progresses.

## Overview
This Singly Android SDK repository contains two different Android projects.  The first is the Singly Android client library.  This is an Android Library project you can include into your Android apps that makes it easy to use the Singly API.  The second in an examples Android project that show useage of the Singly client and various components.

The Singly Android client is a library supporting the [Singly](https://singly.com) social API that:

  - Allows users to easily authenticate with any service supported by Singly; for example Facebook, Twitter, Github, Foursquare and others
  - Allows making requests to the Singly API to retrieve social data for use in your app

The library code is contained in the SinglyAndroidSDK project in the sdk folder.  The `com.singly.android.client.SinglyClient` class is the entry point to using the Singly API in your Android project.

Sample implementations are contained in the SinglyAndroidExamples project in the examples folder.  Once you add your Singly client id and client secret as described below you can build and deploy the example application to see usage of the SDK.

## Dependencies
The Singly Android SDK supports native Facebook authentication if the user has the Facebook Android app installed.  To support this functionality the Facebook Android SDK must be downloaded and linked as an Android Library project.  The Singly Android SDK already references the Facebook Android SDK as a Library project.  All tha needs to be done is to download the Facebook Android SDK from github at the following location and place it in the same parent directory as the Singly Android SDK:

  https://github.com/facebook/facebook-android-sdk/

## Getting Started

The flow of using the Singly Android SDK is as follows

1. Go to https://singly.com and register or login.
2. Create your app or use the default Singly app.
3. Get the Singly client id and client secret for your app.
4. Set your Singly client id and client secret in the `com.singly.android.client.SinglyClient` private constructor in the source code of the Singly Android SDK library.  The hardcoding of the client id and client secret are for security purposes.
5. Link the Singly Android SDK as a Library project into your application.
6. Register the `com.singly.android.client.AuthenticationActivity` and `com.singly.android.client.FacebookAuthenticationActivity` activities in the AndroidManifest.xml file for your application.
7. Get an instance of the SinglyClient using the `com.singly.android.client.SinglyClient.getInstance()` method.
8. Authenticate a user against one or more services that singly supports.  This gives you a Singly access token.  The one Singly access token allow you to call methods for any service the user is authenticated against.  The Singly access token is stored in SharedPreferences along with a unique account id.
9. Call Singly API methods to retrieve social data.  Parse the JSON responses and use the data in your application.

## Using the Android Client

### Setting Your Singly Keys
To start you will need to setup your Singly client id and client secret.  This is set in the source code of the Singly Android SDK library.  Go to the `com.singly.android.client.SinglyClient` class and in the private constructor you will see two lines like this:

    this.clientId = "your_client_id";
    this.clientSecret = "your_client_secret";

You will need to change those lines to be your client id and client secret.  If you haven't done so yet, follow the Singly app registration process at https://singly.com to obtain your app client id and client secret. 

### Register the Singly Android SDK as a Library
To use the SDK you will need to register the SinglyAndroidSDK project as a Libray in your app.  To do this through Eclipse, right click on your project in the navigator tab.  Select properties from the context menu.  They select the Android link on the Project properties dialog.  Scroll down on the right hand pane.  At the bottom there is a Library pane.  Click add and navigate to the sdk directory of the Singly Android SDK project. 

### Setting up the AndroidManifest.xml file
You will then need to register a few activites and permissions in the `AndroidManifest.xml` file for your app.  First register permissions.  The Singly Android SDK will need to access the internet, network state, and write to internal storage.

  <uses-permission android:name="android.permission.INTERNET" />
  <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
  <uses-permission android:name="android.permission.WRITE_INTERNAL_STORAGE" />

Next register the activities used when calling authenticate on the SinglyClient.  These are required.

    <activity android:name="com.singly.android.client.AuthenticationActivity" />
    <activity android:name="com.singly.android.client.FacebookAuthenticationActivity" />

And finally if using any of the Singly components you would also need to register those activites.  For example if using the Authenticated Services component you would need to register its activity in your AndroidManifest.xml file.

    <activity android:name="com.singly.android.component.AuthenticatedServicesActivity" />

### Instantiating the SinglyClient
Once you have set your client id and client secret you can obtain an instance of the SinglyClient using the getInstance method. 

    SinglyClient api = SinglyClient.getInstance();

The SinglyClient is a singleton.  Only one instance of the client is used throughout an Android application.

### Authenticate the User
To access the Singly API your user must first authenticate with one or more services that Singly supports.  This is done through the `authenticate()` method of the SinglyClient class.  The authenticate method will launch an AuthenticationActivity that handles authenticating the user with the service.  The current Android context and a service name to authenticate against are passed in.  You can replace the service name "facebook" with [any service Singly supports](https://singly.com/docs)

    api.authenticate(context, "facebook");

### Making API calls
Once a user has authenticated with a service you will be able to make api calls to the Singly API to retrieve the user's social data.  This is done throught the `com.singly.android.client.SinglyClient.doXXXApiRequest` methods.  To make an api call you provide the api path and any api parameters.  Access token is not required as it will be appended to any api calls made through the client.
    
    api.doGetApiRequest(context, "/profiles", queryParams, new AsyncApiResponseHandler() {

      public void onSuccess(String response) {
        // the response from the api, usually a JSON string, is passed in
      }

      public void onFailure(Throwable error) {
        // error performing the api request
      }
    });

All api calls are performed asynchronously.  A `com.singly.android.client.AsyncApiResponseHandler` class is provided to callback on the success or error of the call.  Upon success the JSONObject reprsenting the api response is returned to the listener.  This data can then be used within your Android app.

For more information on available Singly api calls check out our [API Overview](https://singly.com/docs/api).

## Java SDK

If you are building a Java desktop or web application, not an Android app, it is better to use the Singly SDK for Java.

https://github.com/Singly/singly-java

Support
--------------

This is a work in progress. If you have questions or comments

* Join our live chatroom at http://support.singly.com
* Email support@singly.com
