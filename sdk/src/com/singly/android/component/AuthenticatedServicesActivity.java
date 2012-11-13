package com.singly.android.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;

import com.singly.android.client.AsyncApiResponseHandler;
import com.singly.android.client.SinglyClient;
import com.singly.android.client.SinglyClient.Authentication;
import com.singly.android.sdk.R;
import com.singly.android.util.ImageCacheListener;
import com.singly.android.util.ImageInfo;
import com.singly.android.util.JSON;
import com.singly.android.util.RemoteImageCache;

/**
 * An Activity that shows which Singly service a user is authenticated against
 * in a ListView.
 * 
 * Each row in the ListView contains the name of the service and a checkbox that
 * is checked if the user is authenticated against that service.  
 * 
 * Clicking an unchecked row will open a dialog that allows the user to choose
 * to authenticate against that service.  If accepted then the user follow the
 * normal authentication process via the authenticate method of the Singly 
 * client which launches an AuthenticationActivity. 
 * 
 * Clicking a checked row will open a dialog that allows the user to choose to
 * remove authentication for the service.  If accepted the Singly profile for 
 * the user for that service is deleted.
 * 
 * Intent values can be passed in as intent extra information to change the
 * behavior of the AuthenticatedServicesActivity component.  They are:
 * 
 * <ol>
 *   <li>scopes - A bundle of service name to oauth scope parameter.</li>
 *   <li>flags - A bundle of service name to oauth flag parameter.</li>
 *   <li>includes - An array of the service names to include.  Only those
 *   services specified will be displayed.  If not set all services Singly 
 *   provides authentication for are displayed.</li>
 *   <li>useNative - True|False should we use native authentication if and when
 *   it is available.</li>
 * </ol>   
 */
public class AuthenticatedServicesActivity
  extends Activity {

  private Context context;
  private SinglyClient singlyClient;
  private AuthenticatedServicesAdapter servicesAdapter;
  private RemoteImageCache imageCache;
  private ListView mainListView;
  private ItemClickListener itemClickListener;

  // list of populated Singly services
  private List<SinglyService> services = new ArrayList<SinglyService>();
  private Set<String> includedServices = new HashSet<String>();

  // Map of service to user id on the service, set of authenticated services
  private Map<String, String> serviceIds = new HashMap<String, String>();
  private Set<String> authServices = new HashSet<String>();

  private class ItemClickListener
    implements OnItemClickListener {

    private Bundle oauthScopes;
    private Bundle oauthFlags;
    private boolean useNativeAuth;

    public ItemClickListener(Bundle oauthScopes, Bundle oauthFlags,
      boolean useNativeAuth) {
      this.oauthScopes = oauthScopes;
      this.oauthFlags = oauthFlags;
      this.useNativeAuth = useNativeAuth;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View item, int pos, long id) {

      // get the service and service name for the clicked row
      SinglyService service = services.get(pos);
      CheckBox checkBox = (CheckBox)item.findViewById(R.id.checkBox1);

      // we want to authenticate if the box currently isn't checked otherwise
      // we want to de-authenticate
      final boolean authenticate = !checkBox.isChecked();
      final String serviceId = service.id;
      final String serviceName = service.name;

      // create the alert ok/cancel dialog
      AlertDialog.Builder okCancelDialog = new AlertDialog.Builder(
        AuthenticatedServicesActivity.this);

      // if authenticating pop dialog to authenticate, else to de-authenticate
      if (authenticate) {
        okCancelDialog.setTitle("Add " + serviceName);
        okCancelDialog.setMessage("Authenticate with " + serviceName);
      }
      else {
        okCancelDialog.setTitle("Remove " + serviceName);
        okCancelDialog.setMessage("Remove authentication for " + serviceName);
      }

      // they clicked the ok button
      okCancelDialog.setPositiveButton("OK",
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dint, int which) {

            if (authenticate) {

              // get oauth scope and flag
              Map<String, String> authExtra = new LinkedHashMap<String, String>();
              if (oauthScopes != null) {
                String serviceScopes = oauthScopes.getString(serviceId);
                if (StringUtils.isNotBlank(serviceScopes)) {
                  authExtra.put("scope", serviceScopes);
                }
              }
              if (oauthFlags != null) {
                String serviceFlags = oauthFlags.getString(serviceId);
                if (StringUtils.isNotBlank(serviceFlags)) {
                  authExtra.put("flag", serviceFlags);
                }
              }

              // not authenticated, follow the normal authentication process
              // via an AuthenticationActivity
              singlyClient.authenticate(context, serviceId, authExtra,
                useNativeAuth);
            }
            else {

              Map<String, String> qparams = new HashMap<String, String>();
              String serviceUserId = serviceIds.get(serviceId);
              qparams.put("delete", serviceUserId + "@" + serviceId);

              // add the client access token
              Authentication auth = singlyClient.getAuthentication(context);
              if (StringUtils.isNotBlank(auth.accessToken)) {
                qparams.put("access_token", auth.accessToken);
              }

              // this calls a profile delete, success means the user is no
              // longer authenticated with the service
              singlyClient.doPostApiRequest(context, "/profiles", qparams,
                new AsyncApiResponseHandler() {

                  @Override
                  public void onSuccess(String response) {

                    // remove the service to id from the mapping
                    serviceIds.remove(serviceId);

                    // remove from authenticated services
                    authServices.remove(serviceId);

                    // update the list view
                    servicesAdapter.notifyDataSetChanged();
                  }

                  @Override
                  public void onFailure(Throwable error) {
                    // nothing on failure, maybe we should show a dialog
                  }
                });
            }
          }
        });

      // they clicked the cancel button
      okCancelDialog.setNegativeButton("Cancel", null);

      // show the dialog
      okCancelDialog.show();
    }
  }

  /**
   * Gets the services that the user is authenticated for and then updates the
   * ListView checkboxes for those services.
   */
  private void updateAuthenticatedServices() {

    // don't get the authenticated services unless we have an access token
    if (!singlyClient.isAuthenticated(this)) {
      return;
    }

    // get the access token and pass it in as a query parameter
    Map<String, String> qparams = new LinkedHashMap<String, String>();
    qparams.put("verify", "true");

    // get the access token
    Authentication auth = singlyClient.getAuthentication(context);
    if (StringUtils.isNotBlank(auth.accessToken)) {
      qparams.put("access_token", auth.accessToken);
    }

    // get all the services the user is authenticated against
    singlyClient.doGetApiRequest(this, "/profiles", qparams,
      new AsyncApiResponseHandler() {

        @Override
        public void onSuccess(String response) {

          // get the set of services from the response and populate the service
          // to user id mapping
          JsonNode root = JSON.parse(response);
          Map<String, JsonNode> profileNodes = JSON.getFields(root);
          for (Map.Entry<String, JsonNode> entry : profileNodes.entrySet()) {

            String profileName = entry.getKey();
            JsonNode profileArrayNode = entry.getValue();

            // ignore the id fiel which is the singly account id
            if (!profileName.equals("id")) {

              // the JSON is an array with a singly node containing the profile
              if (profileArrayNode.isArray()) {

                // check if the auth token for the profile is no longer valid
                // if not valid ignore the service
                JsonNode profileNode = profileArrayNode.get(0);
                JsonNode errorNode = JSON.getJsonNode(profileNode, "error");
                if (errorNode != null) {
                  continue;
                }

                // add the profile name and id
                String profileId = JSON.getString(profileNode, "id");
                serviceIds.put(profileName, profileId);
                authServices.add(profileName);
              }
            }
          }

          // notify the list view that the data has changed, update view
          servicesAdapter.notifyDataSetChanged();
        }

        @Override
        public void onFailure(Throwable error) {

        }
      });
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.singly_authenticated_services);
    this.imageCache = new RemoteImageCache(this, 2, null, 50);

    // set the context and any scopes specified
    this.context = this;
    Intent intent = getIntent();

    // scopes and flags parameters for each service
    Bundle oauthScopes = intent.getBundleExtra("scopes");
    Bundle oauthFlags = intent.getBundleExtra("flags");
    boolean useNativeAuth = intent.getBooleanExtra("useNativeAuth", false);

    // setup a service inclusion list if needed
    String[] includesAr = intent.getStringArrayExtra("includes");
    if (includesAr != null && includesAr.length > 0) {
      includedServices.addAll(Arrays.asList(includesAr));
    }

    // get an instance of the singly client
    singlyClient = SinglyClient.getInstance();

    // get the main list view and put a click listener on it. This will tell
    // us which row was clicked, on the layout xml the checkbox is not focusable
    // or clickable directly, the row handles that
    mainListView = (ListView)findViewById(R.id.authenticatedServicesList);
    this.itemClickListener = new ItemClickListener(oauthScopes, oauthFlags,
      useNativeAuth);
    mainListView.setOnItemClickListener(itemClickListener);

    // set the services array adapter into the main list view
    servicesAdapter = new AuthenticatedServicesAdapter(this, services,
      authServices, imageCache);
    mainListView.setAdapter(servicesAdapter);
  }

  @Override
  protected void onStart() {

    super.onStart();

    // do a call to singly to get all the available services
    singlyClient.doGetApiRequest(this, "/services", null,
      new AsyncApiResponseHandler() {

        @Override
        public void onSuccess(String response) {

          // new list of services
          List<SinglyService> curServices = new ArrayList<SinglyService>();
          boolean onlyIncluded = !includedServices.isEmpty();

          JsonNode rootNode = JSON.parse(response);
          Map<String, JsonNode> serviceNodes = JSON.getFields(rootNode);

          // loop through the service name to objects
          for (Map.Entry<String, JsonNode> entry : serviceNodes.entrySet()) {

            // parse and add the service to the services list
            JsonNode serviceNode = entry.getValue();
            SinglyService singlyService = new SinglyService();
            singlyService.id = entry.getKey();
            singlyService.name = StringUtils.capitalize(JSON.getString(
              serviceNode, "name"));

            // if we have an include set only use services in the set
            if (onlyIncluded && !includedServices.contains(singlyService.id)) {
              continue;
            }

            // create a map of the icons and their sizes
            Map<String, String> icons = new HashMap<String, String>();
            List<JsonNode> iconNodes = JSON.getJsonNodes(serviceNode, "icons");
            for (JsonNode iconNode : iconNodes) {
              int height = JSON.getInt(iconNode, "height");
              int width = JSON.getInt(iconNode, "width");
              String source = JSON.getString(iconNode, "source");
              String key = height + "x" + width;
              icons.put(key, source);
            }
            singlyService.icons = icons;

            // if possible retrieve a previously downloaded icon, if not then
            // download and store it in an async manner
            ImageInfo imageInfo = new ImageInfo();
            String id = StringUtils.lowerCase(singlyService.id + "_icon_32x32");
            imageInfo.id = id;
            imageInfo.imageUrl = singlyService.icons.get("32x32");
            imageInfo.width = 32;
            imageInfo.height = 32;
            imageInfo.sample = false;

            singlyService.imageInfo = imageInfo;

            // callback that updates the singly image in a singly row if that
            // row is visible when the image is finished downloading.
            imageInfo.listener = new ImageCacheListener() {

              @Override
              public void onSuccess(ImageInfo imageInfo, Bitmap bitmap) {

                int startRow = mainListView.getFirstVisiblePosition();
                int endRow = mainListView.getLastVisiblePosition();
                for (int i = startRow; i <= endRow; i++) {
                  SinglyService curService = services.get(i);
                  if (curService.imageInfo == imageInfo) {
                    View rowView = mainListView.getChildAt(i - startRow);
                    ImageView imageView = (ImageView)rowView
                      .findViewById(R.id.iconView1);
                    imageView.setImageBitmap(bitmap);
                    break;
                  }
                }
              }
            };

            curServices.add(singlyService);
          }

          // sort the services by name
          Collections.sort(curServices, new Comparator<SinglyService>() {

            @Override
            public int compare(SinglyService lhs, SinglyService rhs) {
              return lhs.name.compareTo(rhs.name);
            }
          });

          // clear and update the services list
          services.clear();
          services.addAll(curServices);

          // display the changes
          servicesAdapter.notifyDataSetChanged();
          updateAuthenticatedServices();
        }

        @Override
        public void onFailure(Throwable error) {

        }
      });
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    imageCache.shutdown();
  }

}
