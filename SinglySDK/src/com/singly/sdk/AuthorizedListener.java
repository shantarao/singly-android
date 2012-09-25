package com.singly.sdk;

public interface AuthorizedListener {

  public void onAuthorized();
  
  public void onError(String message);
  
  public void onStartAuthDialog();
  
  public void onFinishAuthDialog();
  
}
