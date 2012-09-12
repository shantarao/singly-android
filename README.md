Singly Android Example
=========

Contained within this simple Android example is a library supporting the [Singly](https://singly.com) social API that will let you

  - easily authenticate with any service supported by Singly
  - make requests (GET and POST) to the Singly endpoints to get your users' social data for use in your app


The library code is contained in Singly.java and AuthDialog.java.

Sample implementations are contained in MainActivity.java (authentication) and ProfilesActivity.java (GET request).

**Important:** In order for the example to work, go to https://singly.com, register or login, create an app, and copy and paste your `CLIENT_ID` and `CLIENT_SECRET` into the `Singly()` constructor at the top of MainActivity and ProfilesActivity.java 

Available methods
-----------
Once you have instatiated `Singly(activity, CLIENT_ID, CLIENT_SECRET)`, call `authorize()` as defined below to obtain an access to the *request* methods.

* `Singly(activity, CLIENT_ID, CLIENT_SECRET)` constructor
* `authorize(activity, "facebook", DialogListener())` replace the string "facebook" with [any service Singly supports](https://singly.com/docs). Your user will be shown the service's login/authorization screen and then returned to your application. DialogListener is a Singly.DialogListener that implements onComplete(String access_token) when the authorization is complete.

    `api.authorize(activity, "facebook", new DialogListener() {
        public void onComplete(String token) {
            //Do Something
        }
     });`

* `JSONObject get(String endpoint, String params)` returns a JSONObject of the data Singly returns at that endpoint. It can be filtered wuth the URL Encoded string params as specified on [API Overview](https://singly.com/docs/api)
* `String request(String method, String endpoint, String params, ArrayList<NameValuePair> HTTPHeaders)` a raw HTTP request to the specified Singly endpoint. This can be used, for example, for [sharing a status](https://singly.com/docs/sharing) to all of the authorized services,

Support
--------------

This is a work in progress. If you have questions or comments

* Join our live chatroom at http://support.singly.com
* Email support@singly.com