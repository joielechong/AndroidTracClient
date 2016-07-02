# To enable ProGuard in your project, edit project.properties
# to define the proguard.config property as described in that file.
#
# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in ${sdk.dir}/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the ProGuard
# include property in project.properties.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-dontobfuscate
#-dontnote android.support.**
#-dontwarn android.support.**
#-dontwarn com.google.android.gms.**

-keep class * extends java.util.ListResourceBundle {
    protected java.lang.Object[][] getContents();
}

-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final *** NULL;
}

# Keep SafeParcelable value, needed for reflection. This is required to support backwards
# compatibility of some classes.
-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final *** NULL;
}

# Keep the names of classes/members we need for client functionality.
-keep @interface com.google.android.gms.common.annotation.KeepName
-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}

# Needed for Parcelable/SafeParcelable Creators to not get stripped
-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Needed when building against pre-Marshmallow SDK.
-dontwarn android.security.NetworkSecurityPolicy

# Keep metadata about included modules.
-keep public class com.google.android.gms.dynamite.descriptors.** {
  public <fields>;
}

# Keep the implementation of the flags api for google-play-services-flags

-keep public class com.google.android.gms.flags.impl.FlagProviderImpl {
  public <fields>; public <methods>;
}

-keepattributes Signature
-keepattributes InnerClasses

-keep,includedescriptorclasses class com.google.android.gms.flags.impl.FlagProviderImpl {
    public <fields>; public <methods>;
}

-keep,includedescriptorclasses class com.google.android.gms.flags.impl.FlagProviderImpl {
    public <fields>; public <methods>;
}

-keep,includedescriptorclasses class android.support.design.widget.NavigationView {
    public <fields>; public <methods>;
}

-keep,includedescriptorclasses class android.support.design.widget.TabLayout {
    public <fields>; public <methods>;
}

-keep,includedescriptorclasses class android.support.v4.view.ViewPager {
    public <fields>; public <methods>;
}

-keep,includedescriptorclasses class android.support.design.widget.Snackbar$SnackbarLayout {
    public <fields>; public <methods>;
}

-keep,includedescriptorclasses class android.support.v4.widget.SlidingPaneLayout {
    public <fields>; public <methods>;
}

-keep,includedescriptorclasses class android.support.v4.widget.SwipeRefreshLayout {
    public <fields>; public <methods>;
}

-keep,includedescriptorclasses class android.support.v7.view.menu.ActionMenuItemView {
    public <fields>; public <methods>;
}

-keep,includedescriptorclasses class android.support.v7.widget.ActionBarContainer {
    public <fields>; public <methods>;
}

-keep,includedescriptorclasses class android.support.v7.widget.ActionBarOverlayLayou {
    public <fields>; public <methods>;
}

-keep,includedescriptorclasses class android.support.v7.widget.ActionMenuView {
    public <fields>; public <methods>;
}

-keep,includedescriptorclasses class android.support.v7.widget.FitWindowsFrameLayout  {
    public <fields>; public <methods>;
}

-keep,includedescriptorclasses class android.support.v4.widget.NestedScrollView {
    public <fields>; public <methods>;
}

-keep,includedescriptorclasses class android.support.v7.widget.RecyclerView {
    public <fields>; public <methods>;
}

-keep,includedescriptorclasses class com.google.android.gms.ads.doubleclick.PublisherAdView {
    public <fields>; public <methods>;
}

-keep,includedescriptorclasses class com.google.android.gms.ads.search.SearchAdView {
    public <fields>; public <methods>;
}

-keep,includedescriptorclasses class android.support.v7.widget.SearchView {
    public <fields>; public <methods>;
}

-keep,includedescriptorclasses class com.google.android.gms.ads.formats.NativeAdView {
    public <fields>; public <methods>;
}

-keep,includedescriptorclasses class com.google.android.gms.ads.AdView {
    public <fields>; public <methods>;
}

-keep,includedescriptorclasses class android.support.v7.widget.ViewStubCompat {
    public <fields>; public <methods>;
}

-keep,includedescriptorclasses class android.support.v7.widget.Toolbar  {
    public <fields>; public <methods>;
}

-keep,includedescriptorclasses class com.google.android.gms.ads.NativeExpressAdView  {
    public <fields>; public <methods>;
}

-keep,includedescriptorclasses class android.support.v7.widget.MenuPopupWindow$MenuDropDownListView  {
    public <fields>; public <methods>;
}

-keep,includedescriptorclasses class android.support.v7.widget.ActivityChooserView  {
    public <fields>; public <methods>;
}

-keep,includedescriptorclasses class android.support.v7.widget.ContentFrameLayout  {
    public <fields>; public <methods>;
}

-keep,includedescriptorclasses class android.support.v7.widget.ActionBarOverlayLayout  {
    public <fields>; public <methods>;
}

-keep,includedescriptorclasses class android.support.v4.widget.DrawerLayout  {
    public <fields>; public <methods>;
}

-keep class com.mfvl.trac.client.TcPreference* {
	*;
}

-keep class android.support.design.widget.Snackbar$SnackbarLayout$OnLayoutChangeListener {
	*;
}

-keep class android.support.design.widget.Snackbar$SnackbarLayout$OnAttachStateChangeListener {
	*;
}

-optimizations !field/removal/writeonly,!field/marking/private,!class/merging/*,!code/allocation/variable
