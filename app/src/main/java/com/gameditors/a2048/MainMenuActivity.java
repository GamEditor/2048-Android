package com.gameditors.a2048;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.games.AchievementsClient;
import com.google.android.gms.games.EventsClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.LeaderboardsClient;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayersClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONObject;

import ir.adad.ad.AdadAdListener;
import ir.adad.banner.AdadBannerAd;
import ir.adad.core.Adad;


public class MainMenuActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener
{
    public static boolean mIsMainMenu = true;

    private static int mRows = 4;
    public static int getRows() { return mRows; }

    private final String BACKGROUND_COLOR_KEY = "BackgroundColor";
    public static int mBackgroundColor = 0;

    // Client used to sign in with Google APIs
    public GoogleSignInClient mGoogleSignInClient;

    // Client variables
    public AchievementsClient mAchievementsClient;
    public LeaderboardsClient mLeaderboardsClient;
    public EventsClient mEventsClient;
    public PlayersClient mPlayersClient;

    // request codes we use when invoking an external activity
    public static final int RC_UNUSED = 5001;
    public static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        mIsMainMenu = true;

        Typeface ClearSans_Bold = Typeface.createFromAsset(getResources().getAssets(), "ClearSans-Bold.ttf");

        Button bt4x4 = findViewById(R.id.btn_start_4x4);
        Button bt5x5 = findViewById(R.id.btn_start_5x5);
        Button bt6x6 = findViewById(R.id.btn_start_6x6);

        bt4x4.setTypeface(ClearSans_Bold);
        bt5x5.setTypeface(ClearSans_Bold);
        bt6x6.setTypeface(ClearSans_Bold);

        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        if(activeNetwork != null && activeNetwork.isConnectedOrConnecting())
        {
            Adad.initialize("0fb16c39-0c78-408f-985e-917f3a3d6972");

            ((AdadBannerAd)findViewById(R.id.banner_ad_view)).setAdListener(new AdadAdListener()
            {
                @Override
                public void onLoaded() { }

                @Override
                public void onShowed() { }

                @Override
                public void onActionOccurred(int code) { }

                @Override
                public void onError(int code, String message) { }

                @Override
                public void onClosed() { }
            });
        }

        // Create the client used to sign in to Google services.
        mGoogleSignInClient = GoogleSignIn.getClient(this,
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN).build());

        if(!isSignedIn())
            startSignInIntent();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.settings_color_picker:
                mRows = 4;  // because of its GameView!
                startActivity(new Intent(MainMenuActivity.this, ColorPickerActivity.class));
                break;
            case R.id.settings_sign_out:
                signOut();
                break;
        }
        return false;
    }

    // Buttons:
    public void onButtonsClick(View view)
    {
        switch (view.getId())
        {
            /*
            case R.id.btn_ballz:
                try
                {
                    Intent ballz = new Intent(Intent.ACTION_MAIN);
                    ballz.setComponent(new ComponentName("com.gameditors.ballz","com.unity3d.player.UnityPlayerActivity"));
                    startActivity(ballz);
                }
                catch (ActivityNotFoundException anfe)
                {
                    try
                    {
                        Intent ballzOnCafeBazaar = new Intent(Intent.ACTION_VIEW);
                        ballzOnCafeBazaar.setData(Uri.parse("bazaar://details?id=com.gameditors.ballz"));
                        ballzOnCafeBazaar.setPackage("com.farsitel.bazaar");
                        startActivity(ballzOnCafeBazaar);
                    }
                    catch (ActivityNotFoundException anfe2) // for bazaar activity not found exception
                    {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://cafebazaar.ir/app/com.gameditors.ballz")));
                    }
                }
                break;
                */
            case R.id.btn_start_4x4:
                StartGame(4);
                break;
            case R.id.btn_start_5x5:
                StartGame(5);
                break;
            case R.id.btn_start_6x6:
                StartGame(6);
                break;
            case R.id.btn_show_achievements:
                if(!isSignedIn())
                    startSignInIntent();
                else
                {
                    try
                    {
                        onShowAchievementsRequested();
                    }
                    catch (Exception e)
                    {
                        Toast.makeText(this, getString(R.string.try_again), Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case R.id.btn_show_leaderboards:
                if(!isSignedIn())
                    startSignInIntent();
                else
                {
                    try
                    {
                        onShowLeaderboardsRequested();
                    }
                    catch (Exception e)
                    {
                        Toast.makeText(this, getString(R.string.try_again), Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case R.id.btn_share:
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.get_from_bazaar) + "\n\n" + getString(R.string.url_cafe_bazaar));

                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_title)));
                break;
            case R.id.btn_more_games:
                try
                {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("bazaar://collection?slug=by_author&aid=scientist_studio"));
                    intent.setPackage("com.farsitel.bazaar");
                    startActivity(intent);
                }
                catch (Exception e)
                {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_cafe_bazzar_developer))));
                }
                break;
            case R.id.btn_rate:
                Intent bazaarIntent = new Intent(Intent.ACTION_EDIT);
                bazaarIntent.setData(Uri.parse("bazaar://details?id=com.gameditors.a2048"));
                bazaarIntent.setPackage("com.farsitel.bazaar");

                try
                {
                    startActivity(bazaarIntent);
                }
                catch (Exception e) // for activity not found exception
                {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_cafe_bazaar))));
                }
                break;
            case R.id.btn_social_instagram:
                Intent instagramIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.instagram_page_uri)));
                instagramIntent.setPackage(getString(R.string.instagram_package_name));

                try
                {
                    startActivity(instagramIntent);
                }
                catch (ActivityNotFoundException e)
                {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.instagram_page_uri))));
                }
                catch (Exception e)
                {
                    Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.btn_settings:
                PopupMenu popup = new PopupMenu(this,view);
                popup.setOnMenuItemClickListener(this);// to implement on click event on items of menu
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.menus, popup.getMenu());
                popup.show();
                break;
            case R.id.btn_send_email:
                String[] TO = { getString(R.string.email_support_address) };
                Intent emailIntent = new Intent(Intent.ACTION_SEND);

                emailIntent.setData(Uri.parse("mailto:"));
                emailIntent.setType("text/plain");

                emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject));

                try
                {
                    emailIntent.setPackage("com.google.android.gm");
                    startActivity(emailIntent);
                }
                catch (ActivityNotFoundException ex)
                {
                    emailIntent.setPackage("");
                    startActivity(Intent.createChooser(emailIntent, getString(R.string.email_send_title)));
                }
                catch (Exception e)
                {
                    Toast.makeText(MainMenuActivity.this, getString(R.string.email_client_error), Toast.LENGTH_SHORT).show();
                }
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        mIsMainMenu = true;

        // Since the state of the signed in user can change when the activity is not active
        // it is recommended to try and sign in silently from when the app resumes.
        signInSilently();

        SaveColors();
        LoadColors();
    }

    private void SaveColors()
    {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();

        if(mBackgroundColor < 0)
            editor.putInt(BACKGROUND_COLOR_KEY, mBackgroundColor);

        editor.apply();
    }

    private void LoadColors()
    {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        if(settings.getInt(BACKGROUND_COLOR_KEY, mBackgroundColor) < 0)
            mBackgroundColor = settings.getInt(BACKGROUND_COLOR_KEY, mBackgroundColor);
        else
            mBackgroundColor = getResources().getColor(R.color.colorBackground);
    }

    private void StartGame(int rows)
    {
        mRows = rows;
        mIsMainMenu = false;
        startActivity(new Intent(MainMenuActivity.this, MainActivity.class));
    }

    public void signInSilently()
    {
        mGoogleSignInClient.silentSignIn().addOnCompleteListener(this, new OnCompleteListener<GoogleSignInAccount>()
        {
            @Override
            public void onComplete(@NonNull Task<GoogleSignInAccount> task)
            {
                if (task.isSuccessful())
                    onConnected(task.getResult());
                else
                    onDisconnected();
            }
        });
    }

    private void signOut()
    {
        if (!isSignedIn())
            return;

        mGoogleSignInClient.signOut().addOnCompleteListener(this, new OnCompleteListener<Void>()
        {
            @Override
            public void onComplete(@NonNull Task<Void> task)
            {
                boolean successful = task.isSuccessful();

                onDisconnected();
            }
        });
    }

    public void handleException(Exception e, String details)
    {
        int status = 0;

        if (e instanceof ApiException)
        {
            ApiException apiException = (ApiException) e;
            status = apiException.getStatusCode();
        }

        String message = getString(R.string.status_exception_error, details, status, e);
    }

    public void onConnected(GoogleSignInAccount googleSignInAccount)
    {
        mAchievementsClient = Games.getAchievementsClient(this, googleSignInAccount);
        mLeaderboardsClient = Games.getLeaderboardsClient(this, googleSignInAccount);
        mEventsClient = Games.getEventsClient(this, googleSignInAccount);
        mPlayersClient = Games.getPlayersClient(this, googleSignInAccount);

        // Set the greeting appropriately on main menu
        mPlayersClient.getCurrentPlayer().addOnCompleteListener(new OnCompleteListener<Player>()
        {
            @Override
            public void onComplete(@NonNull Task<Player> task)
            {
                String displayName;
                if (task.isSuccessful())
                    displayName = task.getResult().getDisplayName();
                else
                {
                    Exception e = task.getException();
                    handleException(e, getString(R.string.players_exception));
                }
            }
        });
    }

    public void onDisconnected()
    {
        mAchievementsClient = null;
        mLeaderboardsClient = null;
        mPlayersClient = null;
    }

    private void startSignInIntent()
    {
        startActivityForResult(mGoogleSignInClient.getSignInIntent(), RC_SIGN_IN);
    }

    private boolean isSignedIn()
    {
        return GoogleSignIn.getLastSignedInAccount(this) != null;
    }

    public void onShowAchievementsRequested()
    {
        mAchievementsClient.getAchievementsIntent().addOnSuccessListener(new OnSuccessListener<Intent>()
        {
            @Override
            public void onSuccess(Intent intent)
            {
                startActivityForResult(intent, RC_UNUSED);
            }
        }).addOnFailureListener(new OnFailureListener()
        {
            @Override
            public void onFailure(@NonNull Exception e)
            {
                handleException(e, getString(R.string.achievements_exception));
            }
        });
    }

    public void onShowLeaderboardsRequested()
    {
        mLeaderboardsClient.getAllLeaderboardsIntent().addOnSuccessListener(new OnSuccessListener<Intent>()
        {
            @Override
            public void onSuccess(Intent intent)
            {
                startActivityForResult(intent, RC_UNUSED);
            }
        }).addOnFailureListener(new OnFailureListener()
        {
            @Override
            public void onFailure(@NonNull Exception e)
            {
                handleException(e, getString(R.string.leaderboards_exception));
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == RC_SIGN_IN)
        {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(intent);

            try
            {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                onConnected(account);
            }
            catch (ApiException apiException)
            {
                String message = apiException.getMessage();
                if (message == null || message.isEmpty())
                    message = getString(R.string.signin_other_error);

                onDisconnected();

                new AlertDialog.Builder(this).setMessage(message)
                        .setNeutralButton(android.R.string.ok, null).show();
            }
        }
    }
}