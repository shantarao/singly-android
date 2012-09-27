# Singly Android Client

This repository contains two different Android projects.  The first is the Singly Android client library.  This is a library project you can include into your Android apps that makes it easy to use the Singly API.  The second in an example Android project that show useage of the Singly client.

The Singly Android client is a library supporting the [Singly](https://singly.com) social API that will let you

  - Allow users to easily authenticate with any service supported by Singly; for example Facebook, Twitter, Github, Foursquare and others
  - Make requests to the Singly API to retrieve your users' social data for use in your app


The library code is contained in the SinglySDK project.  The com.singly.sdk.SinglyClient class is the entry point to using the Singly API in your Android project.

Sample implementations are contained in the Singly Android Example project.  The com.singly.android_example.MainActivity and ProfilesActivity show usage of the SinglyClient class to authenticate and perform API calls.

## Registering Your Singly App

In order to use the Android client you will need to do the following:

1. Go to https://singly.com and register or login.
2. Create your app or use the default app.
3. Get the client id and client secret for your app.  
4. If using the examples, copy and past the client id and client secret into the SinglyClient constructor at the top of the MainActivity and ProfilesActivity classes in the Singly Android Example project.
5. If creating your own Andorid app, use the client id and client secret in the SinglyClient constructor.


## Using the Android Client

### Instantiating the SinglyClient
The first step is creating the client class as shown below.  This assume you have gone through the registration process and have obtained your clientId and clientSecret. 

    SinglyClient api = new SinglyClient(activity,
      "your_client_id", 
      "your_client_secret");

### Authenticate the User
To access the Singly API your user must first authenticate with one or more services that Singly supports.  This is done through the `authorize()` method of the SinglyClient class.  The authorize method will launch a WebView Dialog to authenticate the user with the service.  An `AuthorizedListener` class is provides to perform callbacks at various stages on the authentication process.  You can replace the string "facebook" with [any service Singly supports](https://singly.com/docs)

    api.authorize("facebook", new AuthorizedListener() {

      public void onStart() {
        // authorization started
      }

      public void onProgress(int progress) {
        // authentication page load progress
      }

      public void onPageLoaded() {
        // authentication page loaded
      }

      public void onAuthorized() {
        // user successfully authenticated with the service
      }

      public void onError(AuthorizedListener.Errors error) {
        // error during user authentication with the service
      }

      public void onCancel() {
        // user cancelled the authentication
      }
    });

### Retrieve User Social Data
Once a user has authenticated with a service you will be able to make api calls to the Singly API to retrieve the user's social data.  This is done throught the `apiCall` method of the `SinglyClient` class.  All api calls are performed asynchronously.  An `APICallListener` class is provided to callback on the success or error of the call.  Upon success the JSONObject reprsenting the api response is returned to the listener.  This data can then be used within your Android app.
    
    api.apiCall("/profiles", null, new APICallListener() {

      public void onSuccess(JSONObject jsonObj) {
        // api call success, profiles returned as JSONObject, do something
      }

      public void onError(String message) {
        // error getting the profiles
      }
    });

For more information on available Singly api calls check out our [API Overview](https://singly.com/docs/api).

Support
--------------

This is a work in progress. If you have questions or comments

* Join our live chatroom at http://support.singly.com
* Email support@singly.com