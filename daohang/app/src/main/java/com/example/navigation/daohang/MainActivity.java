package com.example.navigation.daohang;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.navisdk.adapter.BNRoutePlanNode;
import com.baidu.navisdk.adapter.BaiduNaviManagerFactory;
import com.baidu.navisdk.adapter.IBNTTSManager;
import com.baidu.navisdk.adapter.IBaiduNaviManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String APP_FOLDER_NAME = "BaiduNavigation";

    private String mSDCardPath = getSDPath();
    private boolean hasInitSuccess = false;
    private LocationClient mLocationClient = null;
    private MyLocationListener mLocationListener = new MyLocationListener();
    private MapView mMapView;
    private BaiduMap mBaiduMap;
    RoutePlanSearch mSearch = null;
    private boolean isFirstLoc = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLocationClient = new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(mLocationListener);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);
        //initDirs();

        List<String>permissionList = new ArrayList<>(); //创建一个空List集合，依次判断后面三个权限有没有被授权，如果没有，则加入List集合中
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);   //通过GPS芯片接收卫星的定位信息，定位精度达10米以内
        }

        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.READ_PHONE_STATE)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.READ_PHONE_STATE);   //访问电话状态
        }

        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE); //允许程序写入外部存储，如SD卡上写文件
        }

        if(!permissionList.isEmpty()){
            String[] permissions = permissionList.toArray(new String[permissionList.size()]);   //将List集合转化为数组
            ActivityCompat.requestPermissions(MainActivity.this,permissions,1); //一次性申请LIst集合中的权限

        }else{
            initLocation();
        }


    }

    // 申请权限
    private void initNavi() {

        BaiduNaviManagerFactory.getBaiduNaviManager().init(this,
                mSDCardPath, APP_FOLDER_NAME, new IBaiduNaviManager.INaviInitListener() {

                    @Override
                    public void onAuthResult(int status, String msg) {
                        String result;
                        if (0 == status) {
                            result = "key校验成功!";
                        } else {
                            result = "key校验失败, " + msg;
                        }
                        Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void initStart() {
                        Toast.makeText(MainActivity.this, "百度导航引擎初始化开始", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void initSuccess() {
                        Toast.makeText(MainActivity.this, "百度导航引擎初始化成功", Toast.LENGTH_SHORT).show();
                        hasInitSuccess = true;
                        mLocationClient.start();
                    }

                    @Override
                    public void initFailed() {
                        Toast.makeText(MainActivity.this, "百度导航引擎初始化失败", Toast.LENGTH_SHORT).show();
                    }
                });


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, int[] grantResults){
        switch (requestCode){
            case 1:
                if(grantResults.length>0){
                    for(int result : grantResults){
                        if(result!=PackageManager.PERMISSION_GRANTED){
                            Toast.makeText(this,"同意所有权限才能正常使用导航",Toast.LENGTH_SHORT).show();
                            finish();   //如果有权限被拒绝，就关闭程序
                            return;
                        }
                    }
                    initLocation();
                }else{
                    Toast.makeText(this, "发生未知错误", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:

        }
    }


    //地图定位初始
    private void initLocation(){
        mMapView = (MapView)findViewById(R.id.mmap);//地图初始化
        mBaiduMap = mMapView.getMap();
        mBaiduMap.setMyLocationEnabled(true);//开启定位图层
        mBaiduMap.setIndoorEnable(true);//获取室内地图
        LocationClientOption mLocationClientOption = new LocationClientOption();
        mLocationClientOption.setCoorType("bd09ll");   //正确显示当前位置必须使用bd09ll，默认为gcj02
        mLocationClientOption.setScanSpan(1000);   //1秒更新当前位置
        mLocationClientOption.setOpenGps(true);
        mLocationClientOption.setIsNeedAddress(true);
        mLocationClientOption.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);   //使用GPS和网络进行定位
        mLocationClient.setLocOption(mLocationClientOption);
        mLocationClient.start();
    }


    //初始化文件夹
    private boolean initDirs() {
        mSDCardPath = getSDPath();
        if (mSDCardPath == null) {
            return false;
        }
        File f = new File(mSDCardPath, APP_FOLDER_NAME);
        if (!f.exists()) {
            try {
                f.mkdir();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    //获取根目录路径
    private String getSDPath() {
        if (Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().toString();
        }
        return null;
    }

    public class MyLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location){
            navigateTo(location);
        }

    }

    private void navigateTo(BDLocation location){
        LatLng point = new LatLng(location.getLatitude(),location.getLongitude());
        BitmapDescriptor bitmap = BitmapDescriptorFactory.fromResource(R.drawable.icon_openmap_mark);
        OverlayOptions option = new MarkerOptions().position(point).icon(bitmap);
        if(isFirstLoc){
            setZoom(point);
            isFirstLoc = false;
        }
        mBaiduMap.addOverlay(option);
    }

    //将定位图标以动画的方式移动到地图中间
    private void setZoom(LatLng point){
            MapStatusUpdate update = MapStatusUpdateFactory.newLatLng(point);
            mBaiduMap.animateMapStatus(update);
            update = MapStatusUpdateFactory.zoomTo(18f);    //地图缩放等级调整为4-21
            mBaiduMap.animateMapStatus(update);//将对象传入该方法中可完成缩放功能
    }


}
