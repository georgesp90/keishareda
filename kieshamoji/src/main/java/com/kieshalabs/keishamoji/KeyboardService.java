package com.kieshalabs.keishamoji;

import android.app.Activity;
import android.app.AppOpsManager;
import android.content.ClipDescription;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.inputmethodservice.InputMethodService;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v13.view.inputmethod.EditorInfoCompat;
import android.support.v13.view.inputmethod.InputConnectionCompat;
import android.support.v13.view.inputmethod.InputContentInfoCompat;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputBinding;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.kieshalabs.keishamoji.packs.PackAdapter;
import com.kieshalabs.keishamoji.packs.PackData;
import com.kieshalabs.keishamoji.stickers.MarginDecoration;
import com.kieshalabs.keishamoji.stickers.StickerAdapter;
import com.kieshalabs.keishamoji.stickers.StickerData;
import com.kieshalabs.keishamoji.stickers.Stickers;
import com.rokolabs.sdk.tools.ThreadUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by mist on 02.12.16.
 */


//public class KeyboardService extends InputMethodService {
public class KeyboardService {
    private static final String TAG = "KeyboardService";
    private final static String SERVICE_NAME = "com.kieshalabs.keishamoji.KeyboardService";
    private static final String MIME_TYPE_GIF = "image/gif";
    private static final String MIME_TYPE_PNG = "image/png";
    public static File imagesDir;
    public static File tempDir;
    private static String authority; //"com.rokolabs.rokomoji.rokomoji";
    private static String DEEPLINK_TEXT = "Check out the Kiehsamoji Keyboard! ";
    private final Context context;
    private final InputMethodService inputMethodService;
    public Stickers stickers;
    LinearLayout mainBoard;
    FrameLayout frameLayout;
    private StickerAdapter stickerAdapter;
    private PackAdapter packAdapter;
    private List<PackData> packDataList;
    private TextView packNameLabel;
    private boolean contentSupportedGif;
    private RecyclerView packView;
    private RecyclerView stickerView;
    private int lastTab = 0;
    private String deeplink = "http://bit.ly/2iZK14F";
    private String deeplinkContentId;
    private long startTime = 0;
    private EditorInfo editorInfo;
    private static KeyboardService instance;

    private KeyboardService(Context context, InputMethodService inputMethodService) {
        this.context = context;
        this.inputMethodService = inputMethodService;
        packDataList = new ArrayList<>();
        packAdapter = new PackAdapter(returnThis(), packDataList);
    }

    public static KeyboardService getInstance() {
        if (instance == null) {
            throw new NullPointerException("KeyboardService must be initialized");
        }
        return instance;
    }

    public static void init(InputMethodService inputMethodService) {
        if (instance != null) {
            throw new IllegalStateException("KeyboardService has been already initialized");
        }
        instance = new KeyboardService(inputMethodService, inputMethodService);
    }

    public void setPackView(RecyclerView packView) {
        this.packView = packView;
        this.packView.setAdapter(packAdapter);
    }

    public static boolean rokomojiEnabled(Context context) {
//        requestPermissionIfNeeded(Manifest.permission.READ_EXTERNAL_STORAGE, activity);
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        List<InputMethodInfo> imList = imm.getEnabledInputMethodList();
        for (InputMethodInfo imi : imList) {

            if (context.getPackageName().equalsIgnoreCase(imi.getPackageName())
                    && SERVICE_NAME.equalsIgnoreCase(imi.getServiceName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean requestPermissionIfNeeded(String permission, Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{permission}, 1);
            return true;
        }
        return false;
    }

    public static void updateDeepLinkText(String text) {
        DEEPLINK_TEXT = text;
    }

    private void showStickers() {
        packDataList.clear();
        if (stickers.packDataList.size() > 0) {
            packDataList.addAll(stickers.packDataList);
        } else {
            packDataList.addAll(stickers.packDataListDefault);
        }
        ThreadUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                packAdapter.notifyDataSetChanged();
                int size = packDataList.size();
                if (size > lastTab) {
                    switchBoard(lastTab);
                } else if (size > 0) {
                    switchBoard(0);
                }
            }
        });
    }

    private KeyboardService returnThis() {
        return this;
    }

    private void getStickers() {
        stickers.loadStickers(new Stickers.CallbackStickersLoaded() {
            @Override
            public void pack() {
                showStickers();
            }
        });
    }

    public void switchBoard(int tab) {
        lastTab = tab;
        if (packDataList.get(tab) == null)
            return;

        if (packNameLabel != null) {
            packNameLabel.setText(packDataList.get(tab).name);
        }

        stickerAdapter = new StickerAdapter(this, packDataList.get(tab).stickers);
        if (stickerView != null) {
            stickerView.setAdapter(stickerAdapter);
        }
    }

    public View onCreateInputView(LayoutInflater inflater) {
        mainBoard = (LinearLayout) inflater.inflate(R.layout.main_board_layout, null);
        //packNameLabel = (TextView) mainBoard.findViewById(R.id.packNameLabel);
        frameLayout = (FrameLayout) mainBoard.findViewById(R.id.gif_view);

        stickerView = (RecyclerView) inflater.inflate(R.layout.recycler_view, null);
        stickerView.addItemDecoration(new MarginDecoration(context));
        stickerView.setHasFixedSize(true);
        stickerView.setLayoutManager(new GridLayoutManager(context, 5));

        frameLayout.addView(stickerView);

        /*ImageView btShareLinkGP = (ImageView) mainBoard.findViewById(R.id.btShareLinkGP);
        btShareLinkGP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shareLinkToGP();
            }
        });*/

        // packs bar
        //packView = (RecyclerView) mainBoard.findViewById(R.id.pack_recycler_view);

        showStickers();
        return mainBoard;
    }

    public void inputContent(@NonNull StickerData stickerData, int position) {
        final int flag;
        //final Uri contentUri = FileProvider.getUriForFile(this, getAuthority(), stickerData.file);
        final Uri contentUri = FileProvider.getUriForFile(context, authority, stickerData.file);

        final EditorInfo editorInfo = inputMethodService.getCurrentInputEditorInfo();
        if (Build.VERSION.SDK_INT >= 25) {
            flag = InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION;
        } else {
            flag = 0;
            try {
                context.grantUriPermission(editorInfo.packageName, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception e) {
                Log.e(TAG, "grantUriPermission failed packageName=" + editorInfo.packageName + " contentUri=" + contentUri, e);
            }
        }

        if (isCommitContentSupported(editorInfo, stickerData.mime)) {
            String description = "Images";
            final InputContentInfoCompat inputContentInfo;
            if (stickerData.url == null) {
                inputContentInfo = new InputContentInfoCompat(contentUri, new ClipDescription(
                        description, new String[]{stickerData.mime, MIME_TYPE_GIF}), null);
            } else {
                inputContentInfo = new InputContentInfoCompat(contentUri, new ClipDescription(
                        description,
                        new String[]{stickerData.mime, MIME_TYPE_GIF}),
                        Uri.parse(stickerData.url));
            }
//            if ("com.facebook.orca".equals(getAppForShare(stickerData).packageName)) {
//                if (!stickerToShare(stickerData)) {
//                    Toast.makeText(this, "Application does not support stic   kers", Toast.LENGTH_SHORT).show();
//                }
//            } else {
            InputConnectionCompat.commitContent(
                    inputMethodService.getCurrentInputConnection(),
                    editorInfo,
                    inputContentInfo, flag, null);
//            }
            // events
            /*Event used = new Event("_ROKO.Stickers. Used");
            used.set("photoType", "New");
            used.set("stickerId", stickerData.objectId);
            used.set("stickerPackId", stickerData.packId);
            used.set("positionInPack", position + 1);
            used.set("stickerPackName", stickerData.packName);
            used.set("imageId", stickerData.imageId);
            used.set("isResized", false);
            RokoLogger.addEvents(used);

            Event placed = new Event("_ROKO.Stickers.Placed");
            placed.set("stickerId", stickerData.objectId);
            placed.set("stickerPackId", stickerData.packId);
            placed.set("stickerPackName", stickerData.packName);
            placed.set("positionInPack", position + 1);
            RokoLogger.addEvents(placed);*/
        } else if (!stickerToShare(stickerData)) {

            Toast.makeText(context, "Application does not support stickers", Toast.LENGTH_SHORT).show();
        }
        //getStickers();
    }

    private Boolean stickerToShare(@NonNull StickerData stickerData, Uri contentUri) {
        ActivityInfo ai = getAppForShare(stickerData);
        if (ai == null) {
            return false;
        } else {
            try {
                Intent share = new Intent(Intent.ACTION_SEND);
                //share.setClassName(ai.applicationInfo.packageName, ai.name);
                share.setType("image/*");
                share.putExtra(Intent.EXTRA_STREAM, contentUri);

                share.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                share.addFlags(Intent.FLAG_FROM_BACKGROUND);

                share.setPackage(ai.packageName);
                context.startActivity(Intent.createChooser(share, "Share Image"));
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    private ActivityInfo getAppForShare(StickerData stickerData) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        //intent.setType("image/gif");
        intent.setType(stickerData.mime);
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
        for (ResolveInfo act : activities) {
            ActivityInfo ai = act.activityInfo;
            //Log.d("###",""+editorInfo.packageName+" :: "+ai.applicationInfo.packageName+" | "+ai.name);
            System.out.println("SSSSSSS: " + ai.applicationInfo.packageName);
            if (editorInfo.packageName.equalsIgnoreCase(ai.applicationInfo.packageName)) {
                return ai;
            }
        }
        return null;
    }

    public void shareLinkToGP() {
        final InputConnection ic = inputMethodService.getCurrentInputConnection();

        if (deeplink != null) {
            ic.commitText(DEEPLINK_TEXT + " " + deeplink, 0);
        }/* else {
            // deeplink
            JSONObject params = new JSONObject();
            try {
                params.put("linkType", "share");
                params.put("isAutogenerated", true);
                RokoLinks.createLink(params, new RokoLinks.CallbackCreateLink() {
                    @Override
                    public void success(ResponseCreateLink responseCreateLink) {
                        deeplink = responseCreateLink.data.link;
                        ic.commitText(DEEPLINK_TEXT + " " + deeplink, 0);
                    }

                    @Override
                    public void failure(String s) {
                        Log.e(TAG, s);
                        ic.commitText(DEEPLINK_TEXT + " http://bit.ly/2iZK14F", 0);
                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        RokoLogger.addEvents(new Event("_ROKO.Stickers.Shared").set("contentId", deeplinkContentId));*/
    }

    public void onStartInputView(EditorInfo info, boolean restarting) {
        /*startTime = System.currentTimeMillis();
        editorInfo = info;
        RokoLogger.addEvents(new Event("_ROKO.Stickers.Entered"));
        getStickers();*/

    }

    public void onFinishInputView(boolean finishingInput) {
        /*if (startTime > 0) {
            long timeSpent = (System.currentTimeMillis() - startTime) / 1000;
            RokoLogger.addEvents(new Event("_ROKO.Stickers.Close").set("Time spent", timeSpent));
        }*/
    }

    private boolean isCommitContentSupported(
            @Nullable final EditorInfo editorInfo, @NonNull final String stickerMimeType) {
        if (editorInfo == null) {
            return false;
        }

        final InputConnection ic = inputMethodService.getCurrentInputConnection();
        if (ic == null) {
            return false;
        }

        if (!validatePackageName(editorInfo)) {
            return false;
        }

        final String[] supportedMimeTypes = EditorInfoCompat.getContentMimeTypes(editorInfo);
//        Toast.makeText(this, "size: " + supportedMimeTypes.length, Toast.LENGTH_SHORT).show();
        return isMimeTypesCompatible(supportedMimeTypes, stickerMimeType);
    }

    public static boolean isMimeTypesCompatible(@NonNull final String[] mimeTypes) {
        // NOTE: let inputContent handle it if the assumption is wrong
        return isMimeTypesCompatible(mimeTypes, MIME_TYPE_PNG);
    }

    public static boolean isMimeTypesCompatible(
            final String[] mimeTypes, final String stickerMimeType) {
        for (String mimeType : mimeTypes) {
            if (ClipDescription.compareMimeTypes(MIME_TYPE_GIF, mimeType)
                    || ClipDescription.compareMimeTypes(stickerMimeType, mimeType)) {
                return true;
            }
        }
        return false;
    }

    private boolean validatePackageName(@Nullable EditorInfo editorInfo) {
        if (editorInfo == null) {
            return false;
        }
        final String packageName = editorInfo.packageName;
        if (packageName == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return true;
        }

        final InputBinding inputBinding = inputMethodService.getCurrentInputBinding();
        if (inputBinding == null) {
            Log.e(TAG, "inputBinding should not be null here. You are likely to be hitting b.android.com/225029");
            return false;
        }
        final int packageUid = inputBinding.getUid();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            final AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            try {
                appOpsManager.checkPackage(packageUid, packageName);
            } catch (Exception e) {
                return false;
            }
            return true;
        }

        final PackageManager packageManager = context.getPackageManager();
        final String possiblePackageNames[] = packageManager.getPackagesForUid(packageUid);
        for (final String possiblePackageName : possiblePackageNames) {
            if (packageName.equals(possiblePackageName)) {
                return true;
            }
        }
        return false;
    }

    public void createDirs(Context context) {
        // context.getCacheDir().getAbsolutePath();

        imagesDir = new File(context.getFilesDir(), "images");
        imagesDir.mkdirs();

        tempDir = new File(context.getFilesDir(), "stickers"); // com/kieshalabs/rokomoji/stickers
        tempDir.mkdirs();
    }

    public void onCreate() {
        //super.onCreate();

        /*SharedPreferences rokoMobiPreferences = context.getSharedPreferences("_RokoMobi", Context.MODE_PRIVATE);
        rokoMobiPreferences.edit().remove("apiUrl").apply();
        rokoMobiPreferences.edit().remove("apiToken").apply();*/

        createDirs(context);

        deeplinkContentId = UUID.randomUUID().toString();
        stickers = new Stickers(context);
        /*RokoMobi.start(context, new RokoMobi.CallbackStart() {
            @Override
            public void start() {
                // deeplink
                authority = RokoMobi.getInstance().getPackageName() + ".rokomoji";
                JSONObject params = new JSONObject();
                try {
                    params.put("linkType", "share");
                    params.put("isAutogenerated", true);
                    RokoLinks.createLink(params, new RokoLinks.CallbackCreateLink() {
                        @Override
                        public void success(ResponseCreateLink responseCreateLink) {
                            deeplink = responseCreateLink.data.link;
                            Log.d(TAG, "deeplink: " + deeplink);
                        }

                        @Override
                        public void failure(String s) {
                            Log.e(TAG, s);
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                // sticker
                getStickers();
            }
        });*/
        authority = context.getPackageName() + ".rokomoji";
    }

    private Boolean stickerToShare(@NonNull StickerData stickerData) {
        if (editorInfo == null) {
            return false;
        }
        Boolean shared = false;
        ActivityInfo ai = getAppForShare(stickerData);
        if (ai == null) {
            return false;
        } else {
            try {

                String imgType = Stickers.getMimeTypeOfFile(stickerData.file.getAbsolutePath());
                String flType = imgType.substring(5).replace("/", "");
                System.out.println("ZZZZZZZ: " + flType);
                File tempFile = new File(tempDir.getPath(), "sticker_" + stickerData.objectId + "." + flType);
                final byte[] buffer = new byte[4096];
                InputStream resourceReader = null;
                OutputStream dataWriter = null;
                try {
                    resourceReader = new FileInputStream(stickerData.file);
                    dataWriter = new FileOutputStream(tempFile);
                    if ("com.whatsapp".equals(ai.packageName)) {
                        tempFile = new File(tempDir.getPath(), "sticker_" + stickerData.objectId + ".jpg");
                        dataWriter = new FileOutputStream(tempFile);
                        BitmapFactory.Options options = Stickers.getBitmapOptions(stickerData.file.getAbsolutePath());
                        Bitmap bitmap = Bitmap.createBitmap(options.outWidth, options.outHeight, Bitmap.Config.ARGB_8888);
                        bitmap.eraseColor(Color.WHITE);
                        Canvas canvas = new Canvas(bitmap);
                        canvas.drawBitmap(BitmapFactory.decodeFile(stickerData.file.getAbsolutePath()), 0, 0, null);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, dataWriter);
                        imgType = "image/jpeg";
                    } else {
                        while (true) {
                            final int numRead = resourceReader.read(buffer);
                            if (numRead <= 0) {
                                break;
                            }
                            dataWriter.write(buffer, 0, numRead);
                        }
                    }
                    //-------------- share
                    Intent intent = new Intent(Intent.ACTION_SEND)
                            //.addCategory(Intent.CATEGORY_LAUNCHER)
                            .setClassName(ai.applicationInfo.packageName, ai.name)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .addFlags(Intent.FLAG_FROM_BACKGROUND)
                            //.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            .setComponent(new ComponentName(ai.applicationInfo.packageName, ai.name));
                    intent.setType(imgType);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        dataWriter.flush();
                        dataWriter.close();
                    }
                    intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context, authority, tempFile));
                    context.getApplicationContext().startActivity(intent);

                    shared = true;
                } finally {
                    if (dataWriter != null) {
                        dataWriter.flush();
                        dataWriter.close();
                    }
                    if (resourceReader != null) {
                        resourceReader.close();
                    }
                }
            } catch (Exception e) {
                System.out.println("AAAAAAAA: " + e.getMessage());
                shared = false;
            }
            return shared;
        }
    }
}

