package com.singly.android.component;

import java.util.List;
import java.util.Set;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.singly.android.sdk.R;
import com.singly.android.util.RemoteImageCache;

/**
 * Adapter class that displays the service name, icon and whether or not the 
 * service is currently authenticated against.
 */
public class AuthenticatedServicesAdapter
  extends ArrayAdapter<SinglyService> {

  private LayoutInflater inflater;
  private List<SinglyService> services;
  private Set<String> authServices;
  private RemoteImageCache imageCache;
  private Bitmap defaultImage;

  /**
   * ViewHolder for optimizing the ListView.
   */
  private static class ServiceViewHolder {
    ImageView icon;
    TextView name;
    CheckBox authenticated;
  }

  public AuthenticatedServicesAdapter(Context context,
    List<SinglyService> services, Set<String> authServices,
    RemoteImageCache imageCache) {

    super(context, R.layout.singly_authenticated_services_row, services);
    this.services = services;
    this.authServices = authServices;

    this.inflater = (LayoutInflater)context
      .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    // image handling
    if (imageCache != null) {
      this.imageCache = imageCache;
      this.defaultImage = BitmapFactory.decodeResource(context.getResources(),
        R.drawable.friend_noimage);
    }

  }

  @Override
  public View getView(int position, View serviceView, ViewGroup parent) {

    ServiceViewHolder viewHolder = null;

    // view holder pattern to optimize loading of the ListView, the inflater
    // only runs once per row
    if (serviceView == null) {

      serviceView = inflater.inflate(
        R.layout.singly_authenticated_services_row, parent, false);

      TextView textView = (TextView)serviceView.findViewById(R.id.textView1);
      CheckBox checkBox = (CheckBox)serviceView.findViewById(R.id.checkBox1);
      ImageView imageView = (ImageView)serviceView.findViewById(R.id.iconView1);

      viewHolder = new ServiceViewHolder();
      viewHolder.icon = imageView;
      viewHolder.name = textView;
      viewHolder.authenticated = checkBox;

      serviceView.setTag(viewHolder);
    }
    else {
      viewHolder = (ServiceViewHolder)serviceView.getTag();
    }

    // update the icon, name, and checkbox. This is important to do as the
    // row Views are reused
    SinglyService service = services.get(position);
    Bitmap serviceImage = imageCache.getImage(service.imageInfo);
    if (serviceImage == null) {
      serviceImage = defaultImage;
    }
    viewHolder.icon.setImageBitmap(serviceImage);
    viewHolder.name.setText(service.name);
    viewHolder.authenticated.setChecked(authServices.contains(service.id));

    return serviceView;
  }
}
