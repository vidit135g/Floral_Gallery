package com.absolute.floral.ui;

import android.app.Activity;
import android.app.PendingIntent;
import android.provider.MediaStore;
import java.util.Collections;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ShareCompat;
import androidx.core.app.SharedElementCallback;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.print.PrintHelper;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.transition.Transition;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.davemorrissey.labs.subscaleview.ImageViewState;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.absolute.floral.R;
import com.absolute.floral.adapter.item.InfoRecyclerViewAdapter;
import com.absolute.floral.adapter.item.viewHolder.ViewHolder;
import com.absolute.floral.adapter.item.viewHolder.VideoViewHolder;
import com.absolute.floral.adapter.item.ItemAdapter;
import com.absolute.floral.ui.widget.DismissFrameLayout;
import com.absolute.floral.data.fileOperations.Move;
import com.absolute.floral.data.models.Album;
import com.absolute.floral.data.models.AlbumItem;
import com.absolute.floral.data.fileOperations.FileOperation;
import com.absolute.floral.data.fileOperations.Rename;
import com.absolute.floral.data.models.File_POJO;
import com.absolute.floral.data.models.Gif;
import com.absolute.floral.data.models.Photo;
import com.absolute.floral.data.provider.MediaProvider;
import com.absolute.floral.data.Settings;
import com.absolute.floral.data.models.Video;
import com.absolute.floral.util.ParallaxTransformer;
import com.absolute.floral.util.animators.ColorFade;
import com.absolute.floral.util.MediaType;
import com.absolute.floral.util.SimpleTransitionListener;
import com.absolute.floral.util.Util;

public class ItemActivity extends ThemeableActivity {
    public static final int REQUEST_CODE_DELETE_ITEM = 890;

    public static final int VIEW_IMAGE = 3;
    public static final int FILE_OP_DIALOG_REQUEST = 1;

    public static final String ALBUM_ITEM = "ALBUM_ITEM";
    public static final String ALBUM_ITEM_PATH = "ALBUM_ITEM_PATH";
    public static final String ALBUM = "ALBUM";
    public static final String ALBUM_PATH = "ALBUM_PATH";
    public static final String ITEM_POSITION = "ITEM_POSITION";
    public static final String VIEW_ONLY = "VIEW_ONLY";
    private static final String WAS_SYSTEM_UI_HIDDEN = "WAS_SYSTEM_UI_HIDDEN";
    private static final String IMAGE_VIEW_SAVED_STATE = "IMAGE_VIEW_SAVED_STATE";
    private static final String INFO_DIALOG_SHOWN = "INFO_DIALOG_SHOWN";
    public static final String SHARED_ELEMENT_RETURN_TRANSITION = "SHARED_ELEMENT_RETURN_TRANSITION";

    private Toolbar toolbar;
    private View bottomBar;
    private ViewPager viewPager;
    private ChromeController chrome;

    private AlertDialog infoDialog;
    private Menu menu;

    private boolean systemUiVisible = true;

    private Album album;
    private AlbumItem albumItem;

    public boolean view_only;

    private boolean isReturning;

    private final SharedElementCallback sharedElementCallback = new SharedElementCallback() {
        @Override
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            if (isReturning) {
                ViewGroup v = viewPager != null ? viewPager.findViewWithTag(albumItem.getPath()) : null;
                View sharedElement = v != null ? v.findViewById(R.id.image) : null;
                if (sharedElement == null) {
                    names.clear();
                    sharedElements.clear();
                } else {
                    names.clear();
                    names.add(sharedElement.getTransitionName());
                    sharedElements.clear();
                    sharedElements.put(sharedElement.getTransitionName(), sharedElement);
                }
            }
        }
    };

    private final SimpleTransitionListener transitionListener
            = new SimpleTransitionListener() {
        @Override
        public void onTransitionStart(@NonNull Transition transition) {
            if (chrome != null) chrome.setVisible(false, false);
            super.onTransitionStart(transition);
        }

        @Override
        public void onTransitionEnd(@NonNull Transition transition) {
            ViewHolder viewHolder = ((ItemAdapter)
                    viewPager.getAdapter()).findViewHolderByTag(albumItem.getPath());
            if (viewHolder == null) {
                return;
            }

            if (!isReturning) {
                onShowViewHolder(viewHolder);
            }

            super.onTransitionEnd(transition);
            albumItem.isSharedElement = false;
            if (!isReturning && chrome != null) chrome.setVisible(true, true);
        }
    };

    public interface ViewPagerOnInstantiateItemCallback {
        boolean onInstantiateItem(ViewHolder viewHolder);
    }

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item);

        MediaProvider.checkPermission(this);

        view_only = getIntent().getBooleanExtra(VIEW_ONLY, false);

        if (!view_only && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && showAnimations()) {
            if (savedInstanceState == null) {
                postponeEnterTransition();
            }
            setEnterSharedElementCallback(sharedElementCallback);
            getWindow().getSharedElementEnterTransition().addListener(transitionListener);
        }

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        for (int i = 0; i < toolbar.getChildCount(); i++) {
            View view = toolbar.getChildAt(i);
            if (view instanceof TextView) {
                TextView tv = (TextView) view;
                if (tv.getText().equals(toolbar.getTitle())) {
                    tv.setTypeface(com.absolute.floral.soma.Soma.display(this));
                    tv.setTextColor(0xFFFFFFFF);
                    break;
                }
            }
        }
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        bottomBar = findViewById(R.id.bottom_bar);

        if (view_only) {
            ImageView delete = findViewById(R.id.delete_button);
            ((View) delete.getParent()).setVisibility(View.GONE);

            ImageView edit = findViewById(R.id.edit_button);
            ((View) edit.getParent()).setVisibility(View.GONE);
        }

        final View dragTarget = findViewById(R.id.drag_target);
        final View scrimTop = findViewById(R.id.scrim_top);
        final View scrimBottom = findViewById(R.id.scrim_bottom);

        // edge-to-edge immersive viewer
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        chrome = new ChromeController(getWindow(), getWindow().getDecorView(),
                toolbar, scrimTop, scrimBottom, (View) bottomBar.getParent());

        final int toolbarPadTop = toolbar.getPaddingTop();
        final int barPadBottom = ((View) bottomBar.getParent()).getPaddingBottom();
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root_view),
                (v, insetsCompat) -> {
                    androidx.core.graphics.Insets sb = insetsCompat.getInsets(
                            androidx.core.view.WindowInsetsCompat.Type.systemBars());
                    toolbar.setPadding(toolbar.getPaddingLeft(), toolbarPadTop + sb.top,
                            toolbar.getPaddingRight(), toolbar.getPaddingBottom());
                    View barParent = (View) bottomBar.getParent();
                    barParent.setPadding(barParent.getPaddingLeft(), barParent.getPaddingTop(),
                            barParent.getPaddingRight(), barPadBottom + sb.bottom);
                    return insetsCompat;
                });

        DismissFrameLayout dismiss = findViewById(R.id.root_view);
        dismiss.setTarget(dragTarget);
        dismiss.setDismissListener(new DismissFrameLayout.Listener() {
            @Override public boolean canDismiss() {
                if (view_only || viewPager == null || viewPager.getAdapter() == null) return false;
                ViewHolder vh = ((ItemAdapter) viewPager.getAdapter())
                        .viewHolderAt(viewPager.getCurrentItem());
                return vh == null || vh.isAtRest();
            }
            @Override public void onDrag(float p) {
                if (chrome != null) chrome.setDragProgress(p);
            }
            @Override public void onDismiss() {
                if (chrome != null) chrome.hideForExit();
                dismissToGrid();
            }
            @Override public void onCancelled() {
                if (chrome != null) chrome.setVisible(chrome.isVisible(), true);
            }
            @Override public void onInfoRequested() {
                showInfoDialog();
            }
        });

        if (!view_only) {
            String path;
            if (savedInstanceState != null && savedInstanceState.containsKey(ALBUM_PATH)) {
                path = savedInstanceState.getString(ALBUM_PATH);
            } else {
                path = getIntent().getStringExtra(ALBUM_PATH);
            }
            Log.d("ItemActivity", "loadAlbum() " + path);
            MediaProvider.loadAlbum(this, path,
                    new MediaProvider.OnAlbumLoadedCallback() {
                        @Override
                        public void onAlbumLoaded(Album album) {
                            Log.d("ItemActivity", "onAlbumLoaded()");
                            ItemActivity.this.album = album;
                            ItemActivity.this.onAlbumLoaded(savedInstanceState);
                        }
                    });
        } else {
            album = getIntent().getExtras().getParcelable(ALBUM);
            onAlbumLoaded(savedInstanceState);
        }
    }

    private void onAlbumLoaded(Bundle savedInstanceState) {
        if (albumItem == null) {
            if (savedInstanceState == null) {
                int position = getIntent().getIntExtra(ITEM_POSITION, 0);
                if (album != null && position >= 0 && position < album.getAlbumItems().size()) {
                    albumItem = album.getAlbumItems().get(position);
                    albumItem.isSharedElement = true;
                }
            } else {
                albumItem = savedInstanceState.getParcelable(ALBUM_ITEM);
                if (albumItem != null && albumItem instanceof Photo) {
                    Photo photo = (Photo) albumItem;
                    ImageViewState imageViewState
                            = (ImageViewState) savedInstanceState.getSerializable(IMAGE_VIEW_SAVED_STATE);
                    photo.putImageViewSavedState(imageViewState);
                }
                if (savedInstanceState.getBoolean(INFO_DIALOG_SHOWN, false)) {
                    showInfoDialog();
                }
            }
        }

        if (albumItem == null) {
            return;
        }
        if (albumItem.getPath() != null)
            com.absolute.floral.data.RecentStore.markViewed(this, albumItem.getPath());

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(albumItem.getName());
        }

        viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(new ItemAdapter(album));
        int currentItem = album.getAlbumItems().indexOf(albumItem);
        viewPager.setCurrentItem(currentItem >= 0 ? currentItem : 0, false);
        if (showAnimations()) {
            viewPager.setPageTransformer(false, new ParallaxTransformer());
        }
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            private final int color = ContextCompat.getColor(ItemActivity.this, R.color.white);

            @Override
            public void onPageSelected(int position) {
                //set new AlbumItem
                albumItem = album.getAlbumItems().get(position);
                if (albumItem.getPath() != null)
                    com.absolute.floral.data.RecentStore.markViewed(ItemActivity.this, albumItem.getPath());
                ColorFade.fadeToolbarTitleColor(toolbar, color,
                        new ColorFade.ToolbarTitleFadeCallback() {
                            @Override
                            public void setTitle(Toolbar toolbar) {
                                toolbar.setTitle(albumItem.getName() != null ? albumItem.getName() : "");
                            }
                        });

                ViewHolder viewHolder = ((ItemAdapter) viewPager.getAdapter())
                        .findViewHolderByTag(albumItem.getPath());
                if (viewHolder != null) {
                    onShowViewHolder(viewHolder);
                }
                pauseOffscreenVideos(position);
                refreshFavouriteIcon();
            }
        });

        // safety net — never let a missed shared-element callback freeze the viewer
        viewPager.postDelayed(() -> {
            try { startPostponedEnterTransition(); } catch (Throwable ignored) {}
        }, 350);

        if (!enterTransitionPostponed()) {
            albumItem.isSharedElement = false;
            //there is no sharedElementTransition
            ItemAdapter adapter = (ItemAdapter) viewPager.getAdapter();
            ViewHolder viewHolder = adapter.findViewHolderByTag(albumItem.getPath());
            if (viewHolder != null) {
                onShowViewHolder(viewHolder);
            } else {
                ((ItemAdapter) viewPager.getAdapter())
                        .addOnInstantiateItemCallback(new ViewPagerOnInstantiateItemCallback() {
                            @Override
                            public boolean onInstantiateItem(ViewHolder viewHolder) {
                                if (viewHolder.albumItem.getPath().equals(albumItem.getPath())) {
                                    onShowViewHolder(viewHolder);
                                    return false;
                                }
                                return true;
                            }
                        });
            }
        }
    }

    public void onShowViewHolder(ViewHolder viewHolder) {
        viewHolder.onSharedElementEnter();

        if (menu != null) {
            menu.findItem(R.id.set_as).setVisible(albumItem instanceof Photo);
            menu.findItem(R.id.print).setVisible(albumItem instanceof Photo);
        }

        // set MaxBrightness
        Settings settings = Settings.getInstance(this);
        if (settings.isMaxBrightness()) {
            final Window window = getWindow();
            final WindowManager.LayoutParams layoutParams = window.getAttributes();
            if (albumItem instanceof Photo) {
                // set screenBrightness to max
                layoutParams.screenBrightness = 1.0f;
            } else {
                // restore user preferred screenBrightness => negative value
                layoutParams.screenBrightness = -1.0f;
            }
            window.setAttributes(layoutParams);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.item, menu);
        this.menu = menu;
        if (view_only) {
            menu.findItem(R.id.copy).setVisible(false);
            menu.findItem(R.id.move).setVisible(false);
            menu.findItem(R.id.rename).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem fav = menu.findItem(R.id.favorite);
        if (fav != null && albumItem != null) {
            boolean on = com.absolute.floral.data.FlagStore.favorites(this).contains(albumItem.getPath());
            fav.setIcon(on ? R.drawable.ic_star_white : R.drawable.ic_star_border_white);
        }
        if (theme.isBaseLight()) {
            int black = ContextCompat.getColor(this, R.color.black);
            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                SpannableString s = new SpannableString(item.getTitle());
                s.setSpan(new ForegroundColorSpan(black), 0, s.length(), 0);
                item.setTitle(s);
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.favorite: {
                boolean now = com.absolute.floral.data.FlagStore.favorites(this).toggle(albumItem.getPath());
                item.setIcon(now ? R.drawable.ic_star_white : R.drawable.ic_star_border_white);
                android.widget.Toast.makeText(this, now ? "Added to Favorites" : "Removed from Favorites",
                        android.widget.Toast.LENGTH_SHORT).show();
                break;
            }
            case R.id.archive: {
                boolean now = com.absolute.floral.data.FlagStore.archive(this).toggle(albumItem.getPath());
                android.widget.Toast.makeText(this, now ? "Archived" : "Removed from Archive",
                        android.widget.Toast.LENGTH_SHORT).show();
                break;
            }
            case R.id.set_as:
                setPhotoAs();
                break;
            case R.id.open_with:
                openWith();
                break;
            case R.id.info:
                showInfoDialog();
                break;
            case R.id.share:
                sharePhoto();
                break;
            case R.id.print:
                printPhoto();
                break;
            case R.id.edit:
                editPhoto();
                break;
            case R.id.copy:
            case R.id.move:
                Intent intent = new Intent(this, FileOperationDialogActivity.class);
                intent.setAction(item.getItemId() == R.id.copy ?
                        FileOperationDialogActivity.ACTION_COPY :
                        FileOperationDialogActivity.ACTION_MOVE);
                intent.putExtra(FileOperationDialogActivity.FILES,
                        new String[]{albumItem.getPath()});

                startActivityForResult(intent, FILE_OP_DIALOG_REQUEST);
                break;
            case R.id.rename:
                renameAlbumItem();
                break;
            case R.id.delete:
                showDeleteDialog();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void setPhotoAs() {
        if (!(albumItem instanceof Photo)) {
            return;
        }

        Uri uri = albumItem.getUri(this);

        Intent intent = new Intent(Intent.ACTION_ATTACH_DATA);
        intent.setDataAndType(uri, MediaType.getMimeType(this, uri));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivityForResult(Intent.createChooser(intent,
                    getString(R.string.set_as)), 13);
        } catch (SecurityException se) {
            Toast.makeText(this, "Error (SecurityException)", Toast.LENGTH_SHORT).show();
            se.printStackTrace();
        } catch (ActivityNotFoundException anfe) {
            Toast.makeText(this, "No App found", Toast.LENGTH_SHORT).show();
            anfe.printStackTrace();
        }
    }

    public void openWith() {
        Uri uri = albumItem.getUri(this);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, MediaType.getMimeType(this, uri));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivityForResult(Intent.createChooser(intent,
                    getString(R.string.open_with)), 13);
        } catch (SecurityException se) {
            Toast.makeText(this, "Error (SecurityException)", Toast.LENGTH_SHORT).show();
            se.printStackTrace();
        } catch (ActivityNotFoundException anfe) {
            Toast.makeText(this, getString(R.string.open_with_error, albumItem.getType(this)), Toast.LENGTH_SHORT).show();
            anfe.printStackTrace();
        }
    }

    public void sharePhoto() {
        Uri uri = albumItem.getUri(this);
        if (albumItem.getPath() != null)
            com.absolute.floral.data.RecentStore.markShared(this,
                    java.util.Collections.singletonList(albumItem.getPath()));

        Intent shareIntent = ShareCompat.IntentBuilder.from(this)
                .addStream(uri)
                .setType(MediaType.getMimeType(this, uri))
                .getIntent();

        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        String title = getString(R.string.share_item, albumItem.getType(this));
        if (shareIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(Intent.createChooser(shareIntent, title));
        } else {
            String error = getString(R.string.share_error, albumItem.getType(this));
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        }
    }

    public void printPhoto() {
        if (!(albumItem instanceof Photo)) {
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
            return;
        }

        PrintHelper photoPrinter = new PrintHelper(this);
        photoPrinter.setScaleMode(PrintHelper.SCALE_MODE_FIT);
        try {
            photoPrinter.printBitmap(albumItem.getPath(),
                    albumItem.getUri(this));
        } catch (FileNotFoundException e) {
            Toast.makeText(this, "Error (FileNotFoundException)", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public void editPhoto() {
        Uri uri = albumItem.getUri(this);

        Intent intent = new Intent(Intent.ACTION_EDIT)
                .setDataAndType(uri, MediaType.getMimeType(this, uri))
                .putExtra(EditImageActivity.IMAGE_PATH, albumItem.getPath())
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            if (intent.resolveActivity(getPackageManager()) != null) {
                String title = getString(R.string.edit_item, albumItem.getType(this));
                startActivity(Intent.createChooser(intent, title));
            } else {
                Toast.makeText(this, getString(R.string.edit_error, albumItem.getType(this)),
                        Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException se) {
            Toast.makeText(this, "Error (SecurityException)", Toast.LENGTH_SHORT).show();
            se.printStackTrace();
        }
    }

    public void showDeleteDialog() {
        new AlertDialog.Builder(this, theme.getDialogThemeRes())
                .setTitle(getString(R.string.delete_item, albumItem.getType(this)) + "?")
                .setNegativeButton(getString(R.string.no), null)
                .setPositiveButton(getString(R.string.delete), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        deletePhoto();
                    }
                })
                .create().show();
    }

    public void deletePhoto() {
        if (!MediaProvider.checkPermission(this)) {
            return;
        }

        if (albumItem == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Uri uri = albumItem.getUri(this);
            if (uri != null) {
                try {
                    PendingIntent pi = MediaStore.createDeleteRequest(getContentResolver(), Collections.singletonList(uri));
                    startIntentSenderForResult(pi.getIntentSender(), REQUEST_CODE_DELETE_ITEM, null, 0, 0, 0);
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        final File_POJO[] files = new File_POJO[]{new File_POJO(albumItem.getPath(), true)};

        registerLocalBroadcastReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                unregisterLocalBroadcastReceiver(this);
                switch (intent.getAction()) {
                    case FileOperation.RESULT_DONE:
                        MediaProvider.dataChanged = true;
                        String path = albumItem.getPath();
                        Intent i = new Intent(AlbumActivity.ALBUM_ITEM_REMOVED)
                                .putExtra(ALBUM_ITEM_PATH, path);
                        //notify AlbumActivity
                        LocalBroadcastManager.getInstance(ItemActivity.this).sendBroadcast(i);
                        /*ItemActivity.this.setResult(RESULT_OK);
                        finish();*/

                        album.getAlbumItems().remove(albumItem);
                        viewPager.getAdapter().notifyDataSetChanged();

                        if (album.getAlbumItems().size() == 0) {
                            ItemActivity.this.setResult(RESULT_OK);
                            finish();
                            return;
                        }

                        albumItem = album.getAlbumItems().get(viewPager.getCurrentItem());
                        ItemAdapter adapter = (ItemAdapter) viewPager.getAdapter();
                        ViewHolder viewHolder = adapter.findViewHolderByTag(albumItem.getPath());
                        onShowViewHolder(viewHolder);
                        break;
                    case FileOperation.FAILED:
                        //onBackPressed();
                        break;
                    default:
                        break;
                }
            }
        });
        startService(FileOperation.getDefaultIntent(this, FileOperation.DELETE, files));
    }

    public void renameAlbumItem() {
        File_POJO file = new File_POJO(albumItem.getPath(), true).setName(albumItem.getName());
        Rename.Util.getRenameDialog(this, file, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case FileOperation.RESULT_DONE:
                        //refresh data
                        final Activity activity = ItemActivity.this;

                        String newFilePath = intent.getStringExtra(Rename.NEW_FILE_PATH);
                        if (newFilePath == null) {
                            return;
                        }
                        int index = newFilePath.lastIndexOf("/");
                        final String albumPath = newFilePath.substring(0, index);
                        getIntent().putExtra(ALBUM_PATH, albumPath);
                        getIntent().putExtra(ITEM_POSITION, album.getAlbumItems().indexOf(albumItem));

                        boolean hiddenFolders = Settings.getInstance(activity).getHiddenFolders();
                        new MediaProvider(activity).loadAlbums(activity, hiddenFolders,
                                new MediaProvider.OnMediaLoadedCallback() {
                                    @Override
                                    public void onMediaLoaded(ArrayList<Album> albums) {
                                        //reload activity
                                        MediaProvider.loadAlbum(activity, albumPath,
                                                new MediaProvider.OnAlbumLoadedCallback() {
                                                    @Override
                                                    public void onAlbumLoaded(Album album) {
                                                        ItemActivity.this.albumItem = null;
                                                        ItemActivity.this.album = album;
                                                        ItemActivity.this.onAlbumLoaded(null);

                                                        //notify AlbumActivity
                                                        LocalBroadcastManager.getInstance(ItemActivity.this)
                                                                .sendBroadcast(new Intent(AlbumActivity.ALBUM_ITEM_RENAMED));
                                                    }
                                                });
                                    }

                                    @Override
                                    public void timeout() {
                                        finish();
                                    }

                                    @Override
                                    public void needPermission() {
                                        finish();
                                    }
                                });
                        break;
                    default:
                        break;
                }
            }
        }).show();
    }

    public void showInfoDialog() {
        if (!view_only) {
            try { InfoSheet.show(this, albumItem); return; }
            catch (Throwable ignored) {}
        }
        final InfoRecyclerViewAdapter adapter = new InfoRecyclerViewAdapter();
        boolean exifSupported = adapter.exifSupported(this, albumItem);

        final View rootView = LayoutInflater.from(this)
                .inflate(R.layout.info_dialog_layout,
                        (ViewGroup) findViewById(R.id.root_view), false);

        final View loadingBar = rootView.findViewById(R.id.progress_bar);
        loadingBar.setVisibility(View.VISIBLE);
        final View dialogLayout = rootView.findViewById(R.id.dialog_layout);
        dialogLayout.setVisibility(View.GONE);

        AlertDialog.Builder builder
                = new AlertDialog.Builder(this, theme.getDialogThemeRes())
                .setTitle(getString(R.string.info))
                .setView(rootView)
                .setPositiveButton(R.string.done, null)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        infoDialog = null;
                    }
                });
        if (exifSupported && !view_only) {
            builder.setNeutralButton(R.string.edit_exif, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent intent =
                            new Intent(ItemActivity.this,
                                    ExifEditorActivity.class);
                    intent.putExtra(ExifEditorActivity.ALBUM_ITEM, albumItem);
                    startActivity(intent);
                }
            });
        }
        infoDialog = builder.create();
        infoDialog.show();
        //noinspection ConstantConditions
        /*infoDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);*/

        boolean showColors = (albumItem instanceof Photo || albumItem instanceof Gif) && !view_only;
        adapter.retrieveData(albumItem, showColors,
                new InfoRecyclerViewAdapter.OnDataRetrievedCallback() {
                    @Override
                    public void onDataRetrieved() {
                        ItemActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                RecyclerView recyclerView = rootView.findViewById(R.id.recyclerView);
                                LinearLayoutManager layoutManager = new LinearLayoutManager(ItemActivity.this);
                                recyclerView.setLayoutManager(layoutManager);
                                recyclerView.setAdapter(adapter);

                                final View scrollIndicatorTop = rootView.findViewById(R.id.scroll_indicator_top);
                                final View scrollIndicatorBottom = rootView.findViewById(R.id.scroll_indicator_bottom);

                                recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                                    @Override
                                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                                        super.onScrolled(recyclerView, dx, dy);
                                        scrollIndicatorTop.setVisibility(
                                                recyclerView.canScrollVertically(-1) ?
                                                        View.VISIBLE : View.INVISIBLE);

                                        scrollIndicatorBottom.setVisibility(
                                                recyclerView.canScrollVertically(1) ?
                                                        View.VISIBLE : View.INVISIBLE);
                                    }
                                });

                                loadingBar.setVisibility(View.GONE);
                                dialogLayout.setVisibility(View.VISIBLE);
                            }
                        });
                    }

                    @Override
                    public void failed() {
                        Toast.makeText(getContext(), R.string.error, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public Context getContext() {
                        return ItemActivity.this;
                    }
                });
    }

    public void bottomBarOnClick(final View v) {
        Drawable d = ((ImageView) v).getDrawable();
        if (d instanceof Animatable && showAnimations()) {
            ((Animatable) d).start();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    bottomBarAction(v);
                }
            }, (int) (400 * Util.getAnimatorSpeed(this)));
        } else {
            bottomBarAction(v);
        }
    }

    private void bottomBarAction(View v) {
        switch (v.getId()) {
            case R.id.info_button:
                showInfoDialog();
                break;
            case R.id.share_button:
                sharePhoto();
                break;
            case R.id.favourite_button:
                toggleFavourite();
                break;
            case R.id.edit_button:
                editPhoto();
                break;
            case R.id.delete_button:
                showDeleteDialog();
                break;
            default:
                break;
        }
    }

    private void toggleFavourite() {
        if (albumItem == null || albumItem.getPath() == null) return;
        boolean now = com.absolute.floral.data.FlagStore.favorites(this).toggle(albumItem.getPath());
        ImageView fav = findViewById(R.id.favourite_button);
        if (fav != null) fav.setImageResource(now ? R.drawable.ic_star_white : R.drawable.ic_star_border_white);
        MenuItem favMenu = menu != null ? menu.findItem(R.id.favorite) : null;
        if (favMenu != null) favMenu.setIcon(now ? R.drawable.ic_star_white : R.drawable.ic_star_border_white);
    }

    private void refreshFavouriteIcon() {
        if (albumItem == null) return;
        boolean on = com.absolute.floral.data.FlagStore.favorites(this).contains(albumItem.getPath());
        ImageView fav = findViewById(R.id.favourite_button);
        if (fav != null) fav.setImageResource(on ? R.drawable.ic_star_white : R.drawable.ic_star_border_white);
    }

    /** A video started playing inline — get the app chrome out of the way. */
    public void onVideoInline() {
        if (chrome != null) chrome.setVisible(false, true);
        systemUiVisible = false;
    }

    void pauseOffscreenVideos(int current) {
        if (viewPager == null || viewPager.getAdapter() == null) return;
        for (ViewHolder vh : ((ItemAdapter) viewPager.getAdapter()).viewHolders()) {
            if (vh instanceof VideoViewHolder && vh.getPosition() != current) {
                ((VideoViewHolder) vh).pausePlayback();
            }
        }
    }

    public void imageOnClick() {
        if (chrome != null) {
            chrome.toggle();
            systemUiVisible = chrome.isVisible();
        }
    }

    public static void videoOnClick(Context context, AlbumItem albumItem) {
        if (!(albumItem instanceof Video)) {
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(albumItem.getUri(context), "video/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "No App found to play your video", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void showUI(boolean show) {
        if (chrome != null) chrome.setVisible(show, true);
    }

    @Override
    public void onPermissionGranted() {
        super.onPermissionGranted();
        this.finish();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (albumItem instanceof Photo) {
            View itemView = viewPager.findViewWithTag(albumItem.getPath());
            if (itemView != null) {
                View view = itemView.findViewById(R.id.subsampling);
                if (view instanceof SubsamplingScaleImageView) {
                    SubsamplingScaleImageView imageView = (SubsamplingScaleImageView) view;
                    ImageViewState state = imageView.getState();
                    if (state != null) {
                        outState.putSerializable(IMAGE_VIEW_SAVED_STATE, state);
                    }
                }
            }
        }
        //outState.putParcelable(ALBUM, album);
        outState.putParcelable(ALBUM_ITEM, albumItem);
        outState.putBoolean(WAS_SYSTEM_UI_HIDDEN, !systemUiVisible);
        outState.putBoolean(INFO_DIALOG_SHOWN, infoDialog != null);
    }

    public interface Callback {
        void done();
    }

    @Override
    public void onBackPressed() {
        if (view_only) {
            this.finish();
        } else {
            dismissToGrid();
        }
    }

    /** Animate back to the originating grid cell (shared-element return). */
    private void dismissToGrid() {
        if (isReturning) return;
        if (chrome != null) chrome.hideForExit();
        if (!showAnimations()) {
            setResultAndFinish();
            return;
        }
        // let the view holder do its exit flourish, but never wait forever on it
        final boolean[] done = { false };
        final Runnable finishNow = () -> {
            if (done[0]) return;
            done[0] = true;
            setResultAndFinish();
        };
        if (viewPager != null && viewPager.getAdapter() != null && albumItem != null) {
            ViewHolder viewHolder = ((ItemAdapter)
                    viewPager.getAdapter()).findViewHolderByTag(albumItem.getPath());
            if (viewHolder != null) {
                try {
                    viewHolder.onSharedElementExit(finishNow::run);
                } catch (Throwable ignored) {}
            }
        }
        new Handler().postDelayed(finishNow, 260);
    }

    private boolean finishStarted;

    public void setResultAndFinish() {
        if (finishStarted) return;
        finishStarted = true;
        isReturning = true;
        Intent data = new Intent();
        data.setAction(SHARED_ELEMENT_RETURN_TRANSITION);
        if (album != null) data.putExtra(AlbumActivity.ALBUM_PATH, album.getPath());
        if (viewPager != null)
            data.putExtra(AlbumActivity.EXTRA_CURRENT_ALBUM_POSITION, viewPager.getCurrentItem());
        setResult(RESULT_OK, data);
        if (showAnimations()) {
            ActivityCompat.finishAfterTransition(this);
            // fallback — some return transitions never call back on newer OS versions
            getWindow().getDecorView().postDelayed(() -> {
                if (!isFinishing() && !isDestroyed()) finish();
            }, 400);
        } else {
            finish();
        }
    }

    @Override
    public int getDarkThemeRes() {
        return R.style.CameraRoll_Theme_PhotoView;
    }

    @Override
    public int getLightThemeRes() {
        return R.style.CameraRoll_Theme_Light_PhotoView;
    }

    @Override
    public IntentFilter getBroadcastIntentFilter() {
        IntentFilter filter = FileOperation.Util.getIntentFilter(super.getBroadcastIntentFilter());
        filter.addAction(DATA_CHANGED);
        return filter;
    }

    @Override
    public BroadcastReceiver getDefaultLocalBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case FileOperation.RESULT_DONE:
                        int type = intent.getIntExtra(FileOperation.TYPE, FileOperation.EMPTY);
                        if (type == FileOperation.MOVE) {
                            ArrayList<String> movedFilesPaths = intent
                                    .getStringArrayListExtra(Move.MOVED_FILES_PATHS);
                            for (int i = 0; i < movedFilesPaths.size(); i++) {
                                String path = movedFilesPaths.get(i);
                                //notify AlbumActivity
                                LocalBroadcastManager.getInstance(ItemActivity.this).sendBroadcast(
                                        new Intent(AlbumActivity.ALBUM_ITEM_REMOVED)
                                                .putExtra(ALBUM_ITEM_PATH, path));
                                ItemActivity.this.setResult(RESULT_OK);
                                finish();
                            }
                        }
                        break;
                    case DATA_CHANGED:
                        final int albumItemIndex = album.getAlbumItems().indexOf(albumItem);
                        String albumPath = getIntent().getStringExtra(ALBUM_PATH);
                        MediaProvider.loadAlbum(ItemActivity.this, albumPath,
                                new MediaProvider.OnAlbumLoadedCallback() {
                                    @Override
                                    public void onAlbumLoaded(Album album) {
                                        if (album == null) {
                                            Toast.makeText(ItemActivity.this, "Error: Album null", Toast.LENGTH_SHORT).show();
                                            finish();
                                            return;
                                        }
                                        int index = albumItemIndex;
                                        ItemActivity.this.album = album;
                                        if (index >= album.getAlbumItems().size()) {
                                            index = album.getAlbumItems().size() - 1;
                                        }
                                        if (index >= 0) {
                                            ((ItemAdapter) viewPager.getAdapter()).setAlbum(album);
                                            albumItem = album.getAlbumItems().get(index);
                                            viewPager.getAdapter().notifyDataSetChanged();
                                            viewPager.setCurrentItem(index);

                                            final ActionBar actionBar = getSupportActionBar();
                                            if (actionBar != null) {
                                                actionBar.setTitle(albumItem.getName());
                                            }

                                            ItemAdapter adapter = (ItemAdapter) viewPager.getAdapter();
                                            ViewHolder viewHolder = adapter.findViewHolderByTag(albumItem.getPath());
                                            if (viewHolder != null) {
                                                onShowViewHolder(viewHolder);
                                            } else {
                                                ((ItemAdapter) viewPager.getAdapter())
                                                        .addOnInstantiateItemCallback(new ViewPagerOnInstantiateItemCallback() {
                                                            @Override
                                                            public boolean onInstantiateItem(ViewHolder viewHolder) {
                                                                if (viewHolder.albumItem.getPath().equals(albumItem.getPath())) {
                                                                    onShowViewHolder(viewHolder);
                                                                    return false;
                                                                }
                                                                return true;
                                                            }
                                                        });
                                            }
                                        } else {
                                            finish();
                                        }
                                    }
                                });
                        break;
                    default:
                        break;
                }
            }
        };
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_DELETE_ITEM) {
            if (resultCode == RESULT_OK) {
                MediaProvider.dataChanged = true;
                String path = albumItem.getPath();
                Intent i = new Intent(AlbumActivity.ALBUM_ITEM_REMOVED)
                        .putExtra(ALBUM_ITEM_PATH, path);
                LocalBroadcastManager.getInstance(ItemActivity.this).sendBroadcast(i);

                album.getAlbumItems().remove(albumItem);
                viewPager.getAdapter().notifyDataSetChanged();

                if (album.getAlbumItems().size() == 0) {
                    ItemActivity.this.setResult(RESULT_OK);
                    finish();
                    return;
                }

                albumItem = album.getAlbumItems().get(viewPager.getCurrentItem());
                ItemAdapter adapter = (ItemAdapter) viewPager.getAdapter();
                ViewHolder viewHolder = adapter.findViewHolderByTag(albumItem.getPath());
                onShowViewHolder(viewHolder);
                Toast.makeText(this, R.string.done, Toast.LENGTH_SHORT).show();
            }
        }
    }

}
