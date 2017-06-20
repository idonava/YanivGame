/* Copyright (C) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yaniv.online;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;

import com.google.example.games.basegameutils.BaseGameUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.yaniv.online.R;

import org.w3c.dom.Text;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.TimeUnit;


public class MainActivity extends Activity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener, RealTimeMessageReceivedListener,
        RoomStatusUpdateListener, RoomUpdateListener, OnInvitationReceivedListener {

    /*
     * API INTEGRATION SECTION. This section contains the code that integrates
     * the game with the Google Play game services API.
     */

    final static String TAG = "[Yaniv]";

    // Request codes for the UIs that we show with startActivityForResult:
    final static int RC_SELECT_PLAYERS = 10000;
    final static int RC_INVITATION_INBOX = 10001;
    final static int RC_WAITING_ROOM = 10002;

    // Request code used to invoke sign in user interactions.
    private static final int RC_SIGN_IN = 9001;

    // Client used to interact with Google APIs.
    private GoogleApiClient mGoogleApiClient;

    // Are we currently resolving a connection failure?
    private boolean mResolvingConnectionFailure = false;

    // Has the user clicked the sign-in button?
    private boolean mSignInClicked = false;

    // Set to true to automatically start the sign in flow when the Activity starts.
    // Set to false to require the user to click the button in order to sign in.
    private boolean mAutoStartSignInFlow = true;

    // Room ID where the currently active game is taking place; null if we're
    // not playing.
    String mRoomId = null;

    // Are we playing in multiplayer mode?
    boolean mMultiplayer = false;

    // The participants in the currently active game
    ArrayList<Participant> mParticipants = null;

    // My participant ID in the currently active game
    String mMyId = null;

    // If non-null, this is the id of the invitation we received via the
    // invitation listener
    String mIncomingInvitationId = null;

    int cardsID[] = {R.id.my_card_1, R.id.my_card_2, R.id.my_card_3, R.id.my_card_4, R.id.my_card_5};
    int cardsTopID[] = {R.id.player_top_card_1, R.id.player_top_card_2, R.id.player_top_card_3, R.id.player_top_card_4, R.id.player_top_card_5};
    int cardsRightID[] = {R.id.player_right_card_1, R.id.player_right_card_2, R.id.player_right_card_3, R.id.player_right_card_4, R.id.player_right_card_5};
    int cardsLeftID[] = {R.id.player_left_card_1, R.id.player_left_card_2, R.id.player_left_card_3, R.id.player_left_card_4, R.id.player_left_card_5};
    int droppedID[] = {R.id.dropped_1, R.id.dropped_2, R.id.dropped_3, R.id.dropped_4, R.id.dropped_5};

    private int[] highscores;
    Vector<Integer> myCardsDrop = new Vector<>();
    Vector<String> myCardsDropToString = new Vector<>();
    Vector<Integer> fromDropCardsList = new Vector<>();
    boolean yaniv = false;
    // cell[0] cell[X]                         WHAT AND WHO                                                        EXPLAIN
    //  TYPE
    //   0	    1	Owner in the initial of the game. Anybody on the game when cards over.	Shuffle the cards and resend them on cell 1. Evreybody needs to load the new cards!
    //   1      1	owner only in start	                                                    Sending the self cards to the other players
    //   2   	1	participant when finish playing  - is Cards                            	sending the drop cards and update on each participant
    //   3	    1	participant when finish playing  - take from 0-primary 1-deck           take from the primary or deck (and update in each screen).
    //   3	    2	participant when finish playing  - Cards he take (only if 0)	       	remove the cards from the screen
    //   4	    1	Anybody	                                                                message indicates whether it's a final score or not
    //   4	    2	Anybody	                                                                The score
    //   4	    3	Anybody	                                                                Whos turn
    //   4	    4	Anybody	         CANCEL FOR NOW                                         last turn (taking from the deck or the primary pot).
    //   4	    4	Anybody	                                                                last Drop Type
    //   5	    1	Owner in the initial of the game. Anybody on the game when cards over.	Sending the last drop cards to the primary deck.
    //   6	    1	Anybody when finished is turn send his new cards                    	Sending the new myCards Vector to all participants.
    //   7	    1	Anybody when declare yaniv                                          	Sending a declare message to other participants.


    byte[] mMsgBuf = new byte[5];

    //*/*/*/*/*/*  Yaniv  */*/*/*/*/*/
    private boolean firstRound = true;
    private int turn = 0;
    private TextView tv;
    //True if the participant is the owner of the game
    private boolean owner = false;
    private int numOfMessages = 0;
    private static final Type DATA_TYPE_M_PARTICIPANT_CARDS =
            new TypeToken<Map<String, Vector<Card>>>() {
            }.getType();
    private static final Type DATA_TYPE_CARD_DECK =
            new TypeToken<Cards>() {
            }.getType();
    private static final Type DATA_TYPE_PRIMARY_DECK =
            new TypeToken<Stack<ArrayList<Card>>>() {
            }.getType();
    private static final Type DATA_TYPE_MY_CARDS =
            new TypeToken<Vector<Card>>() {
            }.getType();

    //Participants common objects
    private Stack<ArrayList<Card>> primaryDeck;
    private Cards cardDeck;

    // Score of other participants. We update this as we receive their scores
    // from the network.
    Map<String, Integer> mParticipantScore = new HashMap<String, Integer>();
    Map<String, Vector<Card>> mParticipantCards = new HashMap<String, Vector<Card>>();
    Map<String, String> mParticipantPlayerPosition = new HashMap<String, String>();

    // Participants who sent us their final score.
    Set<String> mFinishedParticipants = new HashSet<String>();
    private int yanivMinScore = 5;
    //Participant objects
    private Vector<Card> myCards;
    private int mySum;
    private int lastDropType = 1;
    private int myLastDropType;
    // private Vector<Vector<Card>> participantsCards = null;
    private int[] invalidDrop = {999};
    private int[] dropCards;
    private int[] takingCard;
    EditText dropCardsEditText;
    EditText takeCardEditText;
    Map<String, Integer> cardsDrawable = new HashMap<String, Integer>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create the Google Api Client with access to Games
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();

        // set up a click listener for everything we care about
        for (int id : CLICKABLES) {
            findViewById(id).setOnClickListener(this);
        }
        tv = (TextView) findViewById(R.id.turn);
        initialCardsDrawable();
    }

    @Override
    public void onClick(View v) {
        Intent intent;

        switch (v.getId()) {
            case R.id.button_single_player:
            case R.id.button_single_player_2:
                // play a single-player game
                resetGameVars();
                startGame(false);
                break;
            case R.id.button_sign_in:
                // user wants to sign in
                // Check to see the developer who's running this sample code read the instructions :-)
                // NOTE: this check is here only because this is a sample! Don't include this
                // check in your actual production app.
                if (!BaseGameUtils.verifySampleSetup(this, R.string.app_id)) {
                    Log.w(TAG, "*** Warning: setup problems detected. Sign in may not work!");
                }

                // start the sign-in flow
                Log.d(TAG, "Sign-in button clicked");
                mSignInClicked = true;
                mGoogleApiClient.connect();
                break;
            case R.id.button_sign_out:
                // user wants to sign out
                // sign out.
                Log.d(TAG, "Sign-out button clicked");
                mSignInClicked = false;
                Games.signOut(mGoogleApiClient);
                mGoogleApiClient.disconnect();
                switchToScreen(R.id.screen_sign_in);
                break;
            case R.id.button_invite_players:
                // show list of invitable players
                intent = Games.RealTimeMultiplayer.getSelectOpponentsIntent(mGoogleApiClient, 1, 3);
                switchToScreen(R.id.screen_wait);
                startActivityForResult(intent, RC_SELECT_PLAYERS);
                break;
            case R.id.button_see_invitations:
                // show list of pending invitations
                intent = Games.Invitations.getInvitationInboxIntent(mGoogleApiClient);
                switchToScreen(R.id.screen_wait);
                startActivityForResult(intent, RC_INVITATION_INBOX);
                break;
            case R.id.button_accept_popup_invitation:
                // user wants to accept the invitation shown on the invitation popup
                // (the one we got through the OnInvitationReceivedListener).
                acceptInviteToRoom(mIncomingInvitationId);
                mIncomingInvitationId = null;
                break;
            case R.id.button_quick_game:
                // user wants to play against a random opponent right now
                startQuickGame();
                break;

        }
    }


    void startQuickGame() {
        // quick-start a game with 1 randomly selected opponent
        final int MIN_OPPONENTS = 1, MAX_OPPONENTS = 1;
        Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(MIN_OPPONENTS,
                MAX_OPPONENTS, 0);
        RoomConfig.Builder rtmConfigBuilder = RoomConfig.builder(this);
        rtmConfigBuilder.setMessageReceivedListener(this);
        rtmConfigBuilder.setRoomStatusUpdateListener(this);
        rtmConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
        switchToScreen(R.id.screen_wait);
        keepScreenOn();
        resetGameVars();
        Games.RealTimeMultiplayer.create(mGoogleApiClient, rtmConfigBuilder.build());
    }

    @Override
    public void onActivityResult(int requestCode, int responseCode,
                                 Intent intent) {
        super.onActivityResult(requestCode, responseCode, intent);

        switch (requestCode) {
            case RC_SELECT_PLAYERS:
                // we got the result from the "select players" UI -- ready to create the room
                handleSelectPlayersResult(responseCode, intent);
                break;
            case RC_INVITATION_INBOX:
                // we got the result from the "select invitation" UI (invitation inbox). We're
                // ready to accept the selected invitation:
                handleInvitationInboxResult(responseCode, intent);
                break;
            case RC_WAITING_ROOM:
                // we got the result from the "waiting room" UI.
                if (responseCode == Activity.RESULT_OK) {
                    // ready to start playing
                    Log.d(TAG, "Starting game (waiting room returned OK).");
                    startGame(true);
                } else if (responseCode == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
                    // player indicated that they want to leave the room
                    leaveRoom();
                } else if (responseCode == Activity.RESULT_CANCELED) {
                    // Dialog was cancelled (user pressed back key, for instance). In our game,
                    // this means leaving the room too. In more elaborate games, this could mean
                    // something else (like minimizing the waiting room UI).
                    leaveRoom();
                }
                break;
            case RC_SIGN_IN:
                Log.d(TAG, "onActivityResult with requestCode == RC_SIGN_IN, responseCode="
                        + responseCode + ", intent=" + intent);
                mSignInClicked = false;
                mResolvingConnectionFailure = false;
                if (responseCode == RESULT_OK) {
                    mGoogleApiClient.connect();
                } else {
                    BaseGameUtils.showActivityResultError(this, requestCode, responseCode, R.string.signin_other_error);
                }
                break;
        }
        super.onActivityResult(requestCode, responseCode, intent);
    }

    // Handle the result of the "Select players UI" we launched when the user clicked the
    // "Invite friends" button. We react by creating a room with those players.
    private void handleSelectPlayersResult(int response, Intent data) {
        if (response != Activity.RESULT_OK) {
            Log.w(TAG, "*** select players UI cancelled, " + response);
            switchToMainScreen();
            return;
        }

        Log.d(TAG, "Select players UI succeeded.");

        // get the invitee list
        final ArrayList<String> invitees = data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);
        Log.d(TAG, "Invitee count: " + invitees.size());

        // get the automatch criteria
        Bundle autoMatchCriteria = null;
        int minAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
        int maxAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);
        if (minAutoMatchPlayers > 0 || maxAutoMatchPlayers > 0) {
            autoMatchCriteria = RoomConfig.createAutoMatchCriteria(
                    minAutoMatchPlayers, maxAutoMatchPlayers, 0);
            Log.d(TAG, "Automatch criteria: " + autoMatchCriteria);
        }

        // create the room
        Log.d(TAG, "Creating room...");
        RoomConfig.Builder rtmConfigBuilder = RoomConfig.builder(this);
        rtmConfigBuilder.addPlayersToInvite(invitees);
        rtmConfigBuilder.setMessageReceivedListener(this);
        rtmConfigBuilder.setRoomStatusUpdateListener(this);
        if (autoMatchCriteria != null) {
            rtmConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
        }
        switchToScreen(R.id.screen_wait);
        keepScreenOn();
        resetGameVars();
        Games.RealTimeMultiplayer.create(mGoogleApiClient, rtmConfigBuilder.build());
        Log.d(TAG, "Room created, waiting for it to be ready...");
    }

    // Handle the result of the invitation inbox UI, where the player can pick an invitation
    // to accept. We react by accepting the selected invitation, if any.
    private void handleInvitationInboxResult(int response, Intent data) {
        if (response != Activity.RESULT_OK) {
            Log.w(TAG, "*** invitation inbox UI cancelled, " + response);
            switchToMainScreen();
            return;
        }

        Log.d(TAG, "Invitation inbox UI succeeded.");
        Invitation inv = data.getExtras().getParcelable(Multiplayer.EXTRA_INVITATION);

        // accept invitation
        acceptInviteToRoom(inv.getInvitationId());
    }

    // Accept the given invitation.
    void acceptInviteToRoom(String invId) {
        // accept the invitation
        Log.d(TAG, "Accepting invitation: " + invId);
        RoomConfig.Builder roomConfigBuilder = RoomConfig.builder(this);
        roomConfigBuilder.setInvitationIdToAccept(invId)
                .setMessageReceivedListener(this)
                .setRoomStatusUpdateListener(this);
        switchToScreen(R.id.screen_wait);
        keepScreenOn();
        resetGameVars();
        Games.RealTimeMultiplayer.join(mGoogleApiClient, roomConfigBuilder.build());
    }

    // Activity is going to the background. We have to leave the current room.
    @Override
    public void onStop() {
        Log.d(TAG, "**** got onStop");

        // if we're in a room, leave it.
      //  leaveRoom();

        // stop trying to keep the screen on
     //   stopKeepingScreenOn();

//        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
//            switchToMainScreen();
//        } else {
//            switchToScreen(R.id.screen_sign_in);
//        }
        super.onStop();
    }

    // Activity just got to the foreground. We switch to the wait screen because we will now
    // go through the sign-in flow (remember that, yes, every time the Activity comes back to the
    // foreground we go through the sign-in flow -- but if the user is already authenticated,
    // this flow simply succeeds and is imperceptible).
    @Override
    public void onStart() {
        if (mGoogleApiClient == null) {
            switchToScreen(R.id.screen_sign_in);
        } else if (!mGoogleApiClient.isConnected()) {
            Log.d(TAG, "Connecting client.");
            switchToScreen(R.id.screen_wait);
            mGoogleApiClient.connect();
        } else {
            Log.w(TAG,
                    "GameHelper: client was already connected on onStart()");
        }
        super.onStart();
        (findViewById(R.id.button_score)).setClickable(false);
    }

    // Handle back key to make sure we cleanly leave a game if we are in the middle of one
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mCurScreen == R.id.screen_game) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Leaving the game")
                    .setMessage("Are you sure you want to leave the game?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            leaveRoom();
                        }

                    })
                    .setNegativeButton("No", null)
                    .show();

            return true;
        }
        return super.onKeyDown(keyCode, e);
    }

    // Leave the room.
    void leaveRoom() {
        Log.d(TAG, "Leaving room.");
        mSecondsLeft = 0;
        stopKeepingScreenOn();
        if (mRoomId != null) {
            Games.RealTimeMultiplayer.leave(mGoogleApiClient, this, mRoomId);
            mRoomId = null;
            switchToScreen(R.id.screen_wait);
        } else {
            switchToMainScreen();
        }
    }

    // Show the waiting room UI to track the progress of other players as they enter the
    // room and get connected.
    void showWaitingRoom(Room room) {
        // minimum number of players required for our game
        // For simplicity, we require everyone to join the game before we start it
        // (this is signaled by Integer.MAX_VALUE).
        final int MIN_PLAYERS = Integer.MAX_VALUE;
        Intent i = Games.RealTimeMultiplayer.getWaitingRoomIntent(mGoogleApiClient, room, MIN_PLAYERS);

        // show waiting room UI
        startActivityForResult(i, RC_WAITING_ROOM);
    }

    // Called when we get an invitation to play a game. We react by showing that to the user.
    @Override
    public void onInvitationReceived(Invitation invitation) {
        // We got an invitation to play a game! So, store it in
        // mIncomingInvitationId
        // and show the popup on the screen.
        mIncomingInvitationId = invitation.getInvitationId();
        ((TextView) findViewById(R.id.incoming_invitation_text)).setText(
                invitation.getInviter().getDisplayName() + " " +
                        getString(R.string.is_inviting_you));
        switchToScreen(mCurScreen); // This will show the invitation popup
    }

    @Override
    public void onInvitationRemoved(String invitationId) {

        if (mIncomingInvitationId.equals(invitationId) && mIncomingInvitationId != null) {
            mIncomingInvitationId = null;
            switchToScreen(mCurScreen); // This will hide the invitation popup
        }

    }

    /*
     * CALLBACKS SECTION. This section shows how we implement the several games
     * API callbacks.
     */

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected() called. Sign in successful!");

        Log.d(TAG, "Sign-in succeeded.");

        // register listener so we are notified if we receive an invitation to play
        // while we are in the game
        Games.Invitations.registerInvitationListener(mGoogleApiClient, this);

        if (connectionHint != null) {
            Log.d(TAG, "onConnected: connection hint provided. Checking for invite.");
            Invitation inv = connectionHint
                    .getParcelable(Multiplayer.EXTRA_INVITATION);
            if (inv != null && inv.getInvitationId() != null) {
                // retrieve and cache the invitation ID
                Log.d(TAG, "onConnected: connection hint has a room invite!");
                acceptInviteToRoom(inv.getInvitationId());
                return;
            }
        }
        switchToMainScreen();

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended() called. Trying to reconnect.");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed() called, result: " + connectionResult);

        if (mResolvingConnectionFailure) {
            Log.d(TAG, "onConnectionFailed() ignoring connection failure; already resolving.");
            return;
        }

        if (mSignInClicked || mAutoStartSignInFlow) {
            mAutoStartSignInFlow = false;
            mSignInClicked = false;
            mResolvingConnectionFailure = BaseGameUtils.resolveConnectionFailure(this, mGoogleApiClient,
                    connectionResult, RC_SIGN_IN, R.string.signin_other_error);
        }

        switchToScreen(R.id.screen_sign_in);
    }

    // Called when we are connected to the room. We're not ready to play yet! (maybe not everybody
    // is connected yet).
    @Override
    public void onConnectedToRoom(Room room) {
        Log.d(TAG, "onConnectedToRoom.");
        initialAllVal();
        //get participants and my ID:
        mParticipants = room.getParticipants();
        mMyId = room.getParticipantId(Games.Players.getCurrentPlayerId(mGoogleApiClient));


        // save room ID if its not initialized in onRoomCreated() so we can leave cleanly before the game starts.
        if (mRoomId == null)
            mRoomId = room.getRoomId();

        // print out the list of participants (for debug purposes)
        Log.d(TAG, "Room ID: " + mRoomId);
        Log.d(TAG, "My ID " + mMyId);
        Log.d(TAG, "<< CONNECTED TO ROOM>>");
    }

    // Called when we've successfully left the room (this happens a result of voluntarily leaving
    // via a call to leaveRoom(). If we get disconnected, we get onDisconnectedFromRoom()).
    @Override
    public void onLeftRoom(int statusCode, String roomId) {
        // we have left the room; return to main screen.
        Log.d(TAG, "onLeftRoom, code " + statusCode);
        switchToMainScreen();
    }

    // Called when we get disconnected from the room. We return to the main screen.
    @Override
    public void onDisconnectedFromRoom(Room room) {
        mRoomId = null;
        showGameError();
    }

    // Show error message about game being cancelled and return to main screen.
    void showGameError() {
        BaseGameUtils.makeSimpleDialog(this, getString(R.string.game_problem));
        switchToMainScreen();
    }

    // Called when room has been created
    @Override
    public void onRoomCreated(int statusCode, Room room) {
        Log.d(TAG, "onRoomCreated(" + statusCode + ", " + room + ")");
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            Log.e(TAG, "*** Error: onRoomCreated, status " + statusCode);
            showGameError();
            return;
        }

        // save room ID so we can leave cleanly before the game starts.
        mRoomId = room.getRoomId();

        // show the waiting room UI
        showWaitingRoom(room);
    }

    // Called when room is fully connected.
    @Override
    public void onRoomConnected(int statusCode, Room room) {

        myCards = new Vector<>();
        initialAllVal();
        //participantsCards = new Vector<Vector<Card>>();
        if (getOwnerId().equals(mMyId)) {
            owner = true;
            ownerInitial();
            updateTurnUi();
        } else {
            owner = false;
            ((TextView) findViewById(R.id.owner)).setText("");


        }
        Log.d(TAG, "onRoomConnected(" + statusCode + ", " + room + ")");
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            Log.e(TAG, "*** Error: onRoomConnected, status " + statusCode);
            showGameError();
            return;
        }
        mySum = 0;
        lastDropType = 1;

        updateRoom(room);
        //Create new suffle deck.
        if (owner) {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //   cardDeck = new Cards();
            Log.d(TAG, "cards(: " + cardDeck.jp);

            //  createParticipantsCards();
            //Sending the participants cards.
            sendParticipantsCardsToAllParticipants();

            updateMyUI();
            // updatePlayersUI();

            //Sending the card deck to all participant
            sendCardDeckToAllParticipants();
            sendPrimaryDeckToAllParticipants();
            //update all participant cards ui

            updateParticipantsNamesAndUI();

        }

    }

    private void sendPrimaryDeckToAllParticipants() {
        Log.d(TAG, "sendPrimaryDeckToAllParticipants() " + primaryDeck);


        byte[] b = new byte[0];

        try {
            b = toGson(primaryDeck, DATA_TYPE_PRIMARY_DECK);
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] sendMsg = new byte[5 + b.length];
        sendMsg[0] = (int) 5;
        for (int i = 0; i < b.length; i++) {
            sendMsg[5 + i] = b[i];
        }
        messageToAllParticipants(sendMsg, true);
        updatePrimaryDeckUI();
        //((TextView) findViewById(R.id.primary_deck)).setText("" + primaryDeck.peek());
    }

    private void sendMyCardsToAllParticipants() {
        Log.d(TAG, "sendMyCardsToAllParticipants() " + myCards);

        byte[] b = new byte[0];

        try {
            b = toGson(myCards, DATA_TYPE_MY_CARDS);
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] sendMsg = new byte[5 + b.length];
        sendMsg[0] = (int) 2;
        for (int i = 0; i < b.length; i++) {
            sendMsg[5 + i] = b[i];
        }
        messageToAllParticipants(sendMsg, true);

    }

    private void sendParticipantsCardsToAllParticipants() {
        Log.d(TAG, "sendParticipantsCardsToAllParticipants() ");
        calculateSum();

        byte[] b = new byte[0];
        try {
            b = toGson(mParticipantCards, DATA_TYPE_M_PARTICIPANT_CARDS);
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] sendMsg = new byte[5 + b.length];
        sendMsg[0] = 1;
        for (int i = 0; i < b.length; i++) {
            sendMsg[5 + i] = b[i];
        }
        messageToAllParticipants(sendMsg, true);

    }

    private void sendCardDeckToAllParticipants() {
        Log.d(TAG, "sendCardDeckToAllParticipants() ");

        byte[] b = new byte[0];

        try {
            b = toGson(cardDeck, DATA_TYPE_CARD_DECK);
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] sendMsg = new byte[5 + b.length];
        sendMsg[0] = (int) 0;
        for (int i = 0; i < b.length; i++) {
            sendMsg[5 + i] = b[i];
        }
        messageToAllParticipants(sendMsg, true);

        //   ((TextView) findViewById(R.id.card_deck)).setText("" + cardDeck.jp);
    }


    private void createParticipantsCards() {
        Log.d(TAG, "createParticipantsCards() ");

        for (int j = 0; j < 5; j++) {
            myCards.add(cardDeck.jp.remove(0));
        }

        mParticipantCards.put(mMyId, myCards);
        for (Participant p : mParticipants) {
            if (p.getParticipantId().equals(mMyId))
                continue;
            if (p.getStatus() != Participant.STATUS_JOINED)
                continue;
            else {

                Vector<Card> v = new Vector<>();
                for (int j = 0; j < 5; j++) {
                    v.add(cardDeck.jp.remove(0));
                }
                mParticipantCards.put(p.getParticipantId(), v);
            }
        }
        setPlayerPositonUI();

    }

    @Override
    public void onJoinedRoom(int statusCode, Room room) {
        Log.d(TAG, "onJoinedRoom(" + statusCode + ", " + room + ")");
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            Log.e(TAG, "*** Error: onRoomConnected, status " + statusCode);
            showGameError();
            return;
        }

        // show the waiting room UI
        showWaitingRoom(room);
    }

    // We treat most of the room update callbacks in the same way: we update our list of
    // participants and update the display. In a real game we would also have to check if that
    // change requires some action like removing the corresponding player avatar from the screen,
    // etc.
    @Override
    public void onPeerDeclined(Room room, List<String> arg1) {
        updateRoom(room);
    }

    @Override
    public void onPeerInvitedToRoom(Room room, List<String> arg1) {
        updateRoom(room);
    }

    @Override
    public void onP2PDisconnected(String participant) {
    }

    @Override
    public void onP2PConnected(String participant) {
    }

    @Override
    public void onPeerJoined(Room room, List<String> arg1) {
        updateRoom(room);
    }

    @Override
    public void onPeerLeft(Room room, List<String> peersWhoLeft) {
        updateRoom(room);
    }

    @Override
    public void onRoomAutoMatching(Room room) {
        updateRoom(room);
    }

    @Override
    public void onRoomConnecting(Room room) {
        updateRoom(room);
    }

    @Override
    public void onPeersConnected(Room room, List<String> peers) {
        updateRoom(room);
    }

    @Override
    public void onPeersDisconnected(Room room, List<String> peers) {
        updateRoom(room);
    }

    void updateRoom(Room room) {
        if (room != null) {
            mParticipants = room.getParticipants();
        }
        if (mParticipants != null) {
            //   updatePeerScoresDisplay();
        }
        if (cardDeck != null) {
            //     ((TextView) findViewById(R.id.card_deck)).setText("" + cardDeck.jp);
        }
        if (primaryDeck != null) {
            //   ((TextView) findViewById(R.id.primary_deck)).setText("" + primaryDeck.peek());
        }

    }

    /*
     * GAME LOGIC SECTION. Methods that implement the game's rules.
     */

    // Current state of the game:
    int mSecondsLeft = -1; // how long until the game ends (seconds)
    final static int GAME_DURATION = 1000; // game duration, seconds.
    int mScore = 0; // user's current score

    // Reset game variables in preparation for a new game.
    void resetGameVars() {
        mSecondsLeft = GAME_DURATION;
        mScore = 0;
        mParticipantScore.clear();
        mFinishedParticipants.clear();
    }

    // Start the gameplay phase of the game.
    void startGame(boolean multiplayer) {
        mMultiplayer = multiplayer;
        //    updateScoreDisplay();
        updateMyUI();
        // broadcastScore(false);
        switchToScreen(R.id.screen_game);

        //findViewById(R.id.button_click_me).setVisibility(View.VISIBLE);

        // run the gameTick() method every second to update the game.
        final Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mSecondsLeft <= 0)
                    return;
                gameTick();
                h.postDelayed(this, 1000);
            }
        }, 1000);
    }

    // Game tick -- update countdown, check if game ended.
    void gameTick() {
        if (mSecondsLeft > 0)
            --mSecondsLeft;

        // update countdown
        ((TextView) findViewById(R.id.countdown)).setText("0:" +
                (mSecondsLeft < 10 ? "0" : "") + String.valueOf(mSecondsLeft));

        if (mSecondsLeft <= 0) {
            // finish game
            //   findViewById(R.id.button_click_me).setVisibility(View.GONE);
            broadcastScore(true);
        }
    }

    // indicates the player scored one point
    void scoreOnePoint() {
        if (!updateTurnUi()) {
            return;
        }

        if (mSecondsLeft <= 0)
            return; // too late!
        ++mScore;
        calculateSum();
        updateMyUI();
        // updateScoreDisplay();
        //   updatePeerScoresDisplay();

        // broadcast our new score to our peers
        broadcastScore(false);
        sendCardDeckToAllParticipants();
        sendMyCardsToAllParticipants();
        sendPrimaryDeckToAllParticipants();
        ((findViewById(R.id.yaniv_declare))).setVisibility(View.GONE);
        ((Button)(findViewById(R.id.button_score))).setText("Score: "+mySum);
    }

    /*
     * COMMUNICATIONS SECTION. Methods that implement the game's network
     * protocol.
     */

    // Called when we receive a real-time message from the network.
    // Messages in our game are made up of 2 bytes: the first one is 'F' or 'U'
    // indicating
    // whether it's a final or interim score. The second byte is the score.
    // There is also the
    // 'S' message, which indicates that the game should start.
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onRealTimeMessageReceived(RealTimeMessage rtm) {

        byte[] buf = rtm.getMessageData();
        String sender = rtm.getSenderParticipantId();
        Log.d(TAG, "[onRealTimeMessageReceived] - Got message from Participant " + sender + ",msg: " + Arrays.toString(buf));

        //  Log.d(TAG, "Message received: " + (int) buf[0] + "/" + (char) buf[1] + "/" + (int) buf[2] + "/" + (int) buf[3]);
        //Game is started, owner send the cardsDeck, any participant needs to reload the cards into cardDeck
        if ((int) buf[0] == 0) {
            cardDeck = fromGson(buf, 5, buf.length, DATA_TYPE_CARD_DECK);
            Log.d(TAG, "[onRealTimeMessageReceived] - cardDeck " + cardDeck.jp);
            if (cardDeck != null) {
                // ((TextView) findViewById(R.id.card_deck)).setText("" + cardDeck.jp);
            }

            //  updateCardDeck();
        }
        // Owner create the cards for all participant. needs to save it on Mycards.
        else if ((int) buf[0] == 1) {
            mParticipantCards = fromGson(buf, 5, buf.length, DATA_TYPE_M_PARTICIPANT_CARDS);
            myCards = mParticipantCards.containsKey(mMyId) ? mParticipantCards.get(mMyId) : null;
            calculateSum();
            setPlayerPositonUI();
            Log.d(TAG, "[onRealTimeMessageReceived] -mycards after" + myCards);
            updateTurnUi();
            updateParticipantsNamesAndUI();
        }
        // When participant finish to play
        else if ((int) buf[0] == 2) {

            mParticipantCards.put(sender, (Vector<Card>) fromGson(buf, 5, buf.length, DATA_TYPE_MY_CARDS));
            Log.d(TAG, "[onRealTimeMessageReceived] -participant " + sender + " finished is turn, his new cards:" + mParticipantCards.get(sender));
            updatePeerScoresDisplay();  //temp - needs to update ONLY the sender.
            updateParticipantUI(sender);
            //  updateCardDeck();
        }
        // take from 0-primary 1-deck           take from the primary or deck (and update in each screen)
        // Cards he take (only if 0)
        else if ((int) buf[0] == 5) {
            primaryDeck = fromGson(buf, 5, buf.length, DATA_TYPE_PRIMARY_DECK);
            Log.d(TAG, "[onRealTimeMessageReceived] - primaryDeck " + primaryDeck);
            if (primaryDeck != null) {
                //  ((TextView) findViewById(R.id.primary_deck)).setText("" + primaryDeck.peek());
                updatePrimaryDeckUI();
            }
        }
        //when player declare yaniv
        else if ((int) buf[0] == 7) {
            updateParticipantsCardsOnGameOverUI();
            lockAllButtons();

        }
        // Regular messages to change the turn.
        else {

            turn = (int) buf[3];
            tv.setText("" + turn);
            updateTurnUi();
            Log.d(TAG, "[onRealTimeMessageReceived] - regular message ");

            if (buf[1] == 'F' || buf[1] == 'U') {
                // score update.
                int existingScore = mParticipantScore.containsKey(sender) ?
                        mParticipantScore.get(sender) : 0;
                int thisScore = (int) buf[2];
                if (thisScore > existingScore) {
                    // this check is necessary because packets may arrive out of
                    // order, so we
                    // should only ever consider the highest score we received, as
                    // we know in our
                    // game there is no way to lose points. If there was a way to
                    // lose points,
                    // we'd have to add a "serial number" to the packet.
                    mParticipantScore.put(sender, thisScore);
                }


                turn = (int) buf[3];
                lastDropType = (int) buf[4];
                // update the scores on the screen
                updatePeerScoresDisplay();

                // if it's a final score, mark this participant as having finished
                // the game
                if ((char) buf[1] == 'F') {
                    mFinishedParticipants.add(rtm.getSenderParticipantId());
                }
            }
            //    updateCardDeck();

        }

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void lockAllButtons() {
        (findViewById(R.id.my_card_1)).setClickable(false);
        (findViewById(R.id.my_card_2)).setClickable(false);
        (findViewById(R.id.my_card_3)).setClickable(false);
        (findViewById(R.id.my_card_4)).setClickable(false);
        (findViewById(R.id.my_card_5)).setClickable(false);
        (findViewById(R.id.dropped_1)).setClickable(false);
        (findViewById(R.id.dropped_3)).setClickable(false);
        (findViewById(R.id.dropped_2)).setClickable(false);
        (findViewById(R.id.dropped_4)).setClickable(false);
        (findViewById(R.id.dropped_5)).setClickable(false);
        (findViewById(R.id.deck_cards)).setClickable(false);
        (findViewById(R.id.my_drop)).setVisibility(View.GONE);
        removeHighLightFromCards();
    }

    private void setPlayerPositonUI() {
        Log.d(TAG, "setPlayerPositonUI()");

        boolean top = false;
        boolean left = false;
        for (Participant p : mParticipants) {
            if (!p.getParticipantId().equals(mMyId)) {

                if (!top) {
                    mParticipantPlayerPosition.put("top", p.getParticipantId());
                    Log.d(TAG, "setPlayerPositonUI() - top - " + p.getParticipantId() + " - " + p.getDisplayName());

                    top = true;
                } else if (!left) {
                    mParticipantPlayerPosition.put("left", p.getParticipantId());
                    Log.d(TAG, "setPlayerPositonUI() - left - " + p.getParticipantId() + " - " + p.getDisplayName());

                    left = true;
                } else {
                    mParticipantPlayerPosition.put("right", p.getParticipantId());
                }
            }
        }
        for (int i = 0; i < cardsTopID.length; i++) {
            (findViewById(cardsTopID[i])).setClickable(false);
            (findViewById(cardsLeftID[i])).setClickable(false);
            (findViewById(cardsRightID[i])).setClickable(false);
        }


    }

    private void updatePrimaryDeckUI() {
        Log.d(TAG, "updatePrimaryDeckUI()");

        int i;
        ImageView myCard;
        ArrayList<Card> peek = primaryDeck.peek();
        for (i = 0; i < peek.size(); i++) {
            int drawable = cardsDrawable.get("" + peek.get(i).getKey());
            myCard = (ImageView) findViewById(droppedID[i]);
            myCard.setImageResource(drawable);
            myCard.setVisibility(View.VISIBLE);

        }
        for (; i < 5; i++) {
            myCard = (ImageView) findViewById(droppedID[i]);
            myCard.setVisibility(View.GONE);
        }
    }


    void messageToAllParticipants(byte[] msg, boolean reliable) {
        Log.d(TAG, "[messageToAllParticipants] Participant " + mMyId + " Send msg to all participants: " + Arrays.toString(msg));

        for (Participant p : mParticipants) {
            if (p.getParticipantId().equals(mMyId))
                continue;
            if (p.getStatus() != Participant.STATUS_JOINED)
                continue;
            if (reliable) {
                // final score notification must be sent via reliable message
                Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, msg,
                        mRoomId, p.getParticipantId());
            } else {
                // it's an interim score notification, so we can use unreliable
                Games.RealTimeMultiplayer.sendUnreliableMessage(mGoogleApiClient, msg, mRoomId,
                        p.getParticipantId());
            }
        }
    }

    // Broadcast my score to everybody else.
    void broadcastScore(boolean finalScore) {
        if (!mMultiplayer)
            return; // playing single-player mode

        mMsgBuf[0] = (byte) 4;
        // First byte in message indicates whether it's a final score or not
        mMsgBuf[1] = (byte) (finalScore ? 'F' : 'U');

        // Second byte is the score.
        mMsgBuf[2] = (byte) mScore;
        turn = ++turn % mParticipants.size();

        mMsgBuf[3] = (byte) (turn);
        Log.d(TAG, "turn= " + turn + " mMsgBuf[3]=" + mMsgBuf[3]);
        mMsgBuf[4] = (byte) (myLastDropType);

        tv.setText("" + turn);
        // Send to every other participant.
        for (Participant p : mParticipants) {
            if (p.getParticipantId().equals(mMyId))
                continue;
            if (p.getStatus() != Participant.STATUS_JOINED)
                continue;
            if (finalScore) {
                Log.d(TAG, "[broadcastScore] Participant " + mMyId + " Send Reliable msg to " + p.getParticipantId() + ",msg: " + Arrays.toString(mMsgBuf));

                // final score notification must be sent via reliable message
                Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, mMsgBuf,
                        mRoomId, p.getParticipantId());
            } else {
                // it's an interim score notification, so we can use unreliable
                Log.d(TAG, "[broadcastScore] Participant " + mMyId + " Send Unreliable msg to " + p.getParticipantId() + ",msg: " + Arrays.toString(mMsgBuf));

                Games.RealTimeMultiplayer.sendUnreliableMessage(mGoogleApiClient, mMsgBuf, mRoomId,
                        p.getParticipantId());
            }
        }
        updateTurnUi();
    }

    /*
     * UI SECTION. Methods that implement the game's UI.
     */

    // This array lists everything that's clickable, so we can install click
    // event handlers.
    final static int[] CLICKABLES = {
            R.id.button_accept_popup_invitation, R.id.button_invite_players,
            R.id.button_quick_game, R.id.button_see_invitations, R.id.button_sign_in,
            R.id.button_sign_out, R.id.button_single_player,
            R.id.button_single_player_2
    };

    // This array lists all the individual screens our game has.
    final static int[] SCREENS = {
            R.id.screen_game, R.id.screen_main, R.id.screen_sign_in,
            R.id.screen_wait
    };
    int mCurScreen = -1;

    void switchToScreen(int screenId) {
        // make the requested screen visible; hide all others.
        for (int id : SCREENS) {
            findViewById(id).setVisibility(screenId == id ? View.VISIBLE : View.GONE);
        }
        mCurScreen = screenId;

        // should we show the invitation popup?
        boolean showInvPopup;
        if (mIncomingInvitationId == null) {
            // no invitation, so no popup
            showInvPopup = false;
        } else if (mMultiplayer) {
            // if in multiplayer, only show invitation on main screen
            showInvPopup = (mCurScreen == R.id.screen_main);
        } else {
            // single-player: show on main screen and gameplay screen
            showInvPopup = (mCurScreen == R.id.screen_main || mCurScreen == R.id.screen_game);
        }
        findViewById(R.id.invitation_popup).setVisibility(showInvPopup ? View.VISIBLE : View.GONE);
    }

    void switchToMainScreen() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            switchToScreen(R.id.screen_main);
        } else {
            switchToScreen(R.id.screen_sign_in);
        }
    }

    // updates the label that shows my score
    void updateScoreDisplay() {
        //  ((TextView) findViewById(R.id.my_score)).setText(formatScore(mScore));
    }

    // formats a score as a three-digit number
    String formatScore(int i) {
        if (i < 0)
            i = 0;
        String s = String.valueOf(i);
        return s.length() == 1 ? "00" + s : s.length() == 2 ? "0" + s : s;
    }

    // updates the screen with the scores from our peers
    void updatePeerScoresDisplay() {
        Log.d(TAG, "updatePeerScoresDisplay() ");

        if (isTurn(mMyId)) {
            (findViewById(R.id.my_card_1)).setClickable(true);
            (findViewById(R.id.my_card_2)).setClickable(true);
            (findViewById(R.id.my_card_3)).setClickable(true);
            (findViewById(R.id.my_card_4)).setClickable(true);
            (findViewById(R.id.my_card_5)).setClickable(true);
            (findViewById(R.id.dropped_1)).setClickable(true);
            (findViewById(R.id.dropped_3)).setClickable(true);
            (findViewById(R.id.dropped_2)).setClickable(true);
            (findViewById(R.id.dropped_4)).setClickable(true);
            (findViewById(R.id.dropped_5)).setClickable(true);
            (findViewById(R.id.deck_cards)).setClickable(true);
            (findViewById(R.id.my_drop)).setVisibility(View.VISIBLE);

            ((Button)findViewById(R.id.topPlayIcon)).setVisibility(View.GONE);
            ((Button)findViewById(R.id.rightPlayIcon)).setVisibility(View.GONE);
            ((Button)findViewById(R.id.leftPlayIcon)).setVisibility(View.GONE);
            ((Button)findViewById(R.id.myPlayIcon)).setVisibility(View.VISIBLE);

//            ((TextView)findViewById(R.id.rightName)).setTypeface(null, Typeface.NORMAL);
//            ((TextView)findViewById(R.id.leftName)).setTypeface(null, Typeface.NORMAL);
//            ((TextView)findViewById(R.id.topName)).setTypeface(null, Typeface.NORMAL);

        } else {
            (findViewById(R.id.my_card_1)).setClickable(false);
            (findViewById(R.id.my_card_2)).setClickable(false);
            (findViewById(R.id.my_card_3)).setClickable(false);
            (findViewById(R.id.my_card_4)).setClickable(false);
            (findViewById(R.id.my_card_5)).setClickable(false);
            (findViewById(R.id.dropped_1)).setClickable(false);
            (findViewById(R.id.dropped_3)).setClickable(false);
            (findViewById(R.id.dropped_2)).setClickable(false);
            (findViewById(R.id.dropped_4)).setClickable(false);
            (findViewById(R.id.dropped_5)).setClickable(false);
            (findViewById(R.id.deck_cards)).setClickable(false);
            (findViewById(R.id.my_drop)).setVisibility(View.GONE);

            if (isTurn(mParticipantPlayerPosition.get("top"))) {
                ((Button)findViewById(R.id.topPlayIcon)).setVisibility(View.VISIBLE);
                ((Button)findViewById(R.id.rightPlayIcon)).setVisibility(View.GONE);
                ((Button)findViewById(R.id.leftPlayIcon)).setVisibility(View.GONE);
                ((Button)findViewById(R.id.myPlayIcon)).setVisibility(View.GONE);
//                ((TextView)findViewById(R.id.topName)).setTypeface(null, Typeface.BOLD);
//                Log.d(TAG, "updateTurnUi: top: " +mParticipantPlayerPosition.get("top"));
//                ((TextView)findViewById(R.id.rightName)).setTypeface(null, Typeface.NORMAL);
//                ((TextView)findViewById(R.id.leftName)).setTypeface(null, Typeface.NORMAL);


            } else if (isTurn(mParticipantPlayerPosition.get("left"))) {
                ((Button)findViewById(R.id.topPlayIcon)).setVisibility(View.GONE);
                ((Button)findViewById(R.id.rightPlayIcon)).setVisibility(View.GONE);
                ((Button)findViewById(R.id.leftPlayIcon)).setVisibility(View.VISIBLE);
                ((Button)findViewById(R.id.myPlayIcon)).setVisibility(View.GONE);
//                ((TextView)findViewById(R.id.leftName)).setTypeface(null, Typeface.BOLD);
//                Log.d(TAG, "updateTurnUi: left: " +mParticipantPlayerPosition.get("left"));
//                ((TextView)findViewById(R.id.topName)).setTypeface(null, Typeface.NORMAL);
//                ((TextView)findViewById(R.id.rightName)).setTypeface(null, Typeface.NORMAL);


            } else {
                ((Button)findViewById(R.id.topPlayIcon)).setVisibility(View.GONE);
                ((Button)findViewById(R.id.rightPlayIcon)).setVisibility(View.VISIBLE);
                ((Button)findViewById(R.id.leftPlayIcon)).setVisibility(View.GONE);
                ((Button)findViewById(R.id.myPlayIcon)).setVisibility(View.GONE);
//                ((TextView)findViewById(R.id.rightName)).setTypeface(null, Typeface.BOLD);
//                Log.d(TAG, "updateTurnUi: right: " +mParticipantPlayerPosition.get("right"));
//                ((TextView)findViewById(R.id.topName)).setTypeface(null, Typeface.NORMAL);
//                ((TextView)findViewById(R.id.leftName)).setTypeface(null, Typeface.NORMAL);


            }
        }


    }

    /*
     * MISC SECTION. Miscellaneous methods.
     */


    // Sets the flag to keep this screen on. It's recommended to do that during
    // the
    // handshake when setting up a game, because if the screen turns off, the
    // game will be
    // cancelled.
    void keepScreenOn() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    // Clears the flag that keeps the screen on.
    void stopKeepingScreenOn() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    String getOwnerId() {
        return mParticipants.get(0).getParticipantId();
    }

    boolean updateTurnUi() {
        Log.d(TAG, "updateTurnUi: mparti0: " + mParticipants.get(0).getParticipantId());
        Log.d(TAG, "updateTurnUi: mpart1i: " + mParticipants.get(1).getParticipantId());
        Log.d(TAG, "updateTurnUi: mpart0icards: " + mParticipantCards.get(mParticipants.get(0).getParticipantId()));
        Log.d(TAG, "updateTurnUi: mpart1icards: " + mParticipantCards.get(mParticipants.get(1).getParticipantId()));

        Log.d(TAG, "updateTurnUi: myid: " + mMyId);
        Log.d(TAG, "updateTurnUi: turn: " + turn);
        updateMyUI();
        updatePeerScoresDisplay();

/*
        if (!mParticipants.get(turn).getParticipantId().equals(mMyId)) {
            ((TextView) findViewById(R.id.button_click_me)).setText("Wait for your turn");
            findViewById(R.id.button_click_me).setEnabled(false);
            ((TextView) findViewById(R.id.score0)).setText(formatScore(mScore) + " - Me - " + myCards);
            return false;
        } else {
            findViewById(R.id.button_click_me).setEnabled(true);
            ((TextView) findViewById(R.id.button_click_me)).setText("Play");
            ((TextView) findViewById(R.id.score0)).setText("-> " + formatScore(mScore) + " - Me - " + myCards);
            return true;
        }
        */
        return true;
    }


    boolean isTurn(String participant) {
        if (!mParticipants.get(turn).getParticipantId().equals(participant)) {
            Log.d(TAG, "myTurn=false");

            return false;
        } else {
            Log.d(TAG, "myTurn=true");

            return true;
        }
    }

    public static byte[] toByte(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toByteArray();
    }

    public static Object fromByte(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }

    //*/*/*/*/*/*/*/*   Yaniv Game Functions   */*/*/*/*/*/*/*//
    public void initialAllVal() {
        myCards = new Vector<>();
        mySum = 0;
        lastDropType = 0;
        //  mParticipantScore = new HashMap<String, Integer>();
        mParticipantCards = new HashMap<String, Vector<Card>>();
        //  mFinishedParticipants = new HashSet<String>();
        primaryDeck = new Stack<>();
        cardDeck = new Cards();
        mParticipantScore = new HashMap<String, Integer>();
        mParticipantPlayerPosition = new HashMap<String, String>();
        myCardsDrop = new Vector<>();
        myCardsDropToString = new Vector<>();
        yaniv = false;
    }

    //Owner Functions
    //First Initial
    void ownerInitial() {
        //((TextView) findViewById(R.id.owner)).setText("owner");

        highscores = new int[mParticipants.size()];
        this.cardDeck = new Cards();
        this.primaryDeck = new Stack<>();
        // Add the first card to primary deck.
        ArrayList<Card> list = new ArrayList<Card>() {
            {
                add(cardDeck.jp.remove(0));
            }
        };
        primaryDeck.add(list);

        // Division card to participants.
        createParticipantsCards();


    }

    public void updateMyUI() {
        int i;
        ImageView myCard;
        for (i = 0; i < myCards.size(); i++) {
            int drawable = cardsDrawable.get("" + myCards.get(i).getKey());
            myCard = (ImageView) findViewById(cardsID[i]);
            myCard.setImageResource(drawable);
            myCard.setVisibility(View.VISIBLE);

        }
        for (; i < 5; i++) {
            myCard = (ImageView) findViewById(cardsID[i]);
            myCard.setVisibility(View.GONE);
        }
        checkYanivOpportunity();
        //     myCard = (ImageView) findViewById(cardsID[0]);
        //   myCard.setImageResource(R.drawable.c_2_of_diamonds);
   /*     Log.d(TAG, "int_resource: " + this.getResources().getIdentifier("drawable/" + myCards.get(0).getResourceName(), "drawable", getPackageName()));
        Log.d(TAG, "int_arr: " + cardsID[0]);
        Log.d(TAG, ": " + myCards.get(0).getResourceName());*/

    }

    public void updateParticipantUI(String pid) {
        Log.d(TAG, "updateParticipantUI()");

        int i;
        int arr[];
        Log.d(TAG, "updateParticipantUI() - mParticipantPlayerPosition - " + mParticipantPlayerPosition);
        Log.d(TAG, "updateParticipantUI() - pid - " + pid);

        if (mParticipantPlayerPosition.get("top").equals(pid)) {
            arr = cardsTopID;
        } else if (mParticipantPlayerPosition.get("left").equals(pid)) {
            arr = cardsLeftID;

        } else {
            arr = cardsRightID;

        }
        ImageView myCard;
        for (i = 0; i < mParticipantCards.get(pid).size(); i++) {
            int drawable;
            if (yaniv) {
                drawable = cardsDrawable.get("" + mParticipantCards.get(pid).get(i).getKey());
            } else {
                drawable = R.drawable.pile_1;
            }
            myCard = (ImageView) findViewById(arr[i]);
            myCard.setImageResource(drawable);
            myCard.setVisibility(View.VISIBLE);

        }
        for (; i < 5; i++) {
            myCard = (ImageView) findViewById(arr[i]);
            myCard.setVisibility(View.GONE);
        }

    }

    public void updateParticipantsCardsOnGameOverUI() {
        Log.d(TAG, "updateParticipantsCardsOnGameOverUI()");
        int i;
        ImageView myCard;
        for (Participant p : mParticipants) {
            String id = p.getParticipantId();
            if (!id.equals(mMyId)) {
                for (i = 0; i < mParticipantCards.get(id).size(); i++) {
                    int arr[];
                    if (mParticipantPlayerPosition.get("top").equals(id)) {
                        arr = cardsTopID;
                    } else if (mParticipantPlayerPosition.get("left").equals(id)) {
                        arr = cardsLeftID;

                    } else {
                        arr = cardsRightID;
                    }
                    int drawable = cardsDrawable.get("" + mParticipantCards.get(id).get(i).getKey());
                    myCard = (ImageView) findViewById(arr[i]);
                    myCard.setImageResource(drawable);
                    //         myCard.setVisibility(View.VISIBLE);

                }
            }
        }

/*
        ImageView myCard;
        for (i = 0; i < mParticipantCards.size(); i++) {
            int drawable;
            if (yaniv) {
                drawable = cardsDrawable.get("" + mParticipantCards.get(pid).get(i).getKey());
            } else {
                drawable = R.drawable.pile_1;
            }
            myCard = (ImageView) findViewById(arr[i]);
            myCard.setImageResource(drawable);
            myCard.setVisibility(View.VISIBLE);

        }
        for (; i < 5; i++) {
            myCard = (ImageView) findViewById(arr[i]);
            myCard.setVisibility(View.GONE);
        }*/

    }

    public void updateParticipantsNamesAndUI() {
        Log.d(TAG, "updateParticipantsNames()");
        Log.d(TAG, "updateParticipantsNames() - mParticipantPlayerPosition - " + mParticipantPlayerPosition);
        Log.d(TAG, "updateParticipantsNames() - mParticipantPlayerPosition - " + mParticipantPlayerPosition.toString());


        for (Participant p : mParticipants) {
            if (!p.getParticipantId().equals(mMyId)) {
                String pid = p.getParticipantId();
                String a = mParticipantPlayerPosition.get("top");
                Log.d(TAG, "updateParticipantsNames() - a - " + a);
                Log.d(TAG, "updateParticipantsNames() - pid - " + pid);

                if (mParticipantPlayerPosition.get("top").equals(p.getParticipantId())) {
                    ((TextView) findViewById(R.id.topName)).setText(p.getDisplayName());

                } else if (mParticipantPlayerPosition.get("left").equals(p.getParticipantId())) {
                    ((TextView) findViewById(R.id.leftName)).setText(p.getDisplayName());
                } else {
                    ((TextView) findViewById(R.id.rightName)).setText(p.getDisplayName());

                }
            }
        }
        //update all participant cards ui
        for (Participant p : mParticipants) {
            if (!p.getParticipantId().equals(mMyId)) {
                updateParticipantUI(p.getParticipantId());
            }
        }
    }


    public <T> T fromGson(byte[] buf, int start, int finish, Type DATA_TYPE) {
        byte[] dataArray = Arrays.copyOfRange(buf, start, finish);
        String fromGson = null;
        try {
            fromGson = (String) fromByte(dataArray);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "fromGson String: " + fromGson);
        Log.d(TAG, "fromGson dataArray[]: " + Arrays.toString(dataArray));
        Log.d(TAG, "fromGson fullbuf[] to: " + Arrays.toString(buf));
        T t = new Gson().fromJson(fromGson, DATA_TYPE);
        return t;
    }

    public <T> byte[] toGson(T t, Type DATA_TYPE) throws IOException {
        Gson gson = new Gson();
        return toByte(gson.toJson(t, DATA_TYPE));

    }

    void calculateSum() {
        mySum = 0;
        for (Card card : myCards) {
            mySum += card.v;
        }
        ((Button)(findViewById(R.id.button_score))).setText("Score: "+mySum);

    }

    public int[] takeFrom() {
        Log.d(TAG, "takeFrom() called ");
        String[] splitString = takeCardEditText.getText().toString().split(",");
        Log.d(TAG, "takeFrom() - splitString[]: " + Arrays.toString(splitString));

        int[] split = tokensStringToInt(splitString);
        Log.d(TAG, "takeFrom() - splitInt[]: " + Arrays.toString(split));
        if (split == invalidDrop || split.length == 0 || split.length > 1 || split[0] > 1 || split[0] < 0) {
            Log.d(TAG, "invalidInput - Enter 0 to take the card from the Primary Deck or 1 from the Card Deck, Try Again");
            Toast.makeText(this, "You enter illegal paramater to take, Try again. ",
                    Toast.LENGTH_LONG).show();
            return invalidDrop;
        }
        return split;
    }

    public int[] takeFromLastDrop() {
        Log.d(TAG, "takeFromLastDrop() called ");
        String[] splitString = takeCardEditText.getText().toString().split(",");
        int[] split = tokensStringToInt(splitString);

        Log.d(TAG, "takeFromLastDrop() - splitString[]: " + Arrays.toString(splitString));
        Log.d(TAG, "primaryDeck=" + primaryDeck.toString());
        ArrayList<Card> lastDrop = primaryDeck.peek();
        Log.d(TAG, "lastDrop=" + lastDrop.toString());

        Log.d(TAG, "takeFromLastDrop() - splitInt[]: " + Arrays.toString(split));
        if (split == invalidDrop || split.length != 1) {
            Log.d(TAG, "invalidInput - Enter 0 to take the card from the Primary Deck or 1 from the Card Deck, Try Again");
            Toast.makeText(this, "You enter illegal paramater to take, Try again. ",
                    Toast.LENGTH_LONG).show();
            return invalidDrop;
        }

        //Taking card from 3 optional options to take.
        if (lastDropType == 1) {    //case one card - taking the only one card are in primary deck.
            if (split[0] >= 0 && split[0] <= lastDrop.size() - 1) {

            } else {
                return invalidDrop;
            }

        } else if (lastDropType == 2) { //case equal
            if (split[0] >= 0 && split[0] <= lastDrop.size() - 1) {

            } else {
                return invalidDrop;
            }
        } else {   //case order
            if (split[0] == 0 || split[0] == lastDrop.size() - 1) {

            } else {
                return invalidDrop;
            }


        }
        return split;
    }


    private int checkVaildDrop(int[] split) {
        Log.d(TAG, "checkVaildDrop() - split:" + Arrays.toString(split));

        //return 0 - not vaild, 1 - one card, 2 - equal cards, 3- orderd cards
        if (split.length == 1) { // Checking if there is only 1 card.
            Log.d(TAG, "checkVaildDrop() - split.length == 1");

            if (split[0] > myCards.size() - 1) {
                Log.d(TAG, "checkVaildDrop() - split.length == 1 - return 0");

                return 0;
            }
            Log.d(TAG, "checkVaildDrop() - split.length == 1 - return 1");

            return 1;
        } else {
            for (int i = 0; i < split.length; i++) {
                if (split[i] >= myCards.size()) { //Checking out of bounds
                    Log.d(TAG, "checkVaildDrop() - Checking out of bounds - return 0");

                    return 0;
                } else {
                    Log.d(TAG, "checkVaildDrop() - Checking out of bounds - else");

                }
            }
            if (split.length == 2) {  //Checking case of 2 jokers or 2 equals cards
                Log.d(TAG, "checkVaildDrop() - Checking case of 2 jokers or 2 equals cards");

                if (myCards.get(split[0]).n == myCards.get(split[1]).n) {
                    Log.d(TAG, "checkVaildDrop() - Checking case of 2 jokers or 2 equals cards - return 2");

                    return 2;
                }
            }
            if (checkEqualArray(split) == 1) { // Vaild drop of equals cards
                Log.d(TAG, "checkVaildDrop() - Vaild drop of equals cards - return 2");

                return 2;
            } else if (checkOrderArray(split) == 1) {
                Log.d(TAG, "checkVaildDrop() - Vaild drop of order cards - return 3");

                return 3;
            }
            return 0;

        }
    }

    private int checkEqualArray(int Tokens[]) {
        Log.d(TAG, "checkEqualArray() - " + Arrays.toString(Tokens));

        int val = -1;
        for (int i = 0; i < Tokens.length; i++) {
            if (myCards.get(Tokens[i]).v != 0 && myCards.get(Tokens[i]).n != val) {
                if (val == -1) {
                    val = myCards.get(Tokens[i]).n;
                } else if (myCards.get(Tokens[i]).n != val) {
                    return 0;
                }
            }

        }
        return 1;
    }

    private int checkOrderArray(int Tokens[]) {
        Log.d(TAG, "checkOrderArray() - " + Arrays.toString(Tokens));

        char symbol = getSymbol(Tokens);
        int orderArray[] = getOrder(Tokens, symbol);
        for (int i = 0; i < orderArray.length - 1; i++) {

            if (orderArray[i + 1] - orderArray[i] != 1) {
                return 0;
            }
        }
        return 1;
    }

    private int[] getOrder(int Tokens[], char s) {
        int orderArray[] = new int[Tokens.length];
        if ((myCards.get(Tokens[0]).v == 0) && myCards.get(Tokens[Tokens.length - 1]).v == 0) {
            for (int i = 1; i < Tokens.length; i++) {
                if ((myCards.get(Tokens[0]).n == 14 || myCards.get(Tokens[0]).n == 15)) {
                    orderArray[i] = myCards.get(Tokens[i - 1]).n + 1;
                } else if (myCards.get(Tokens[i]).s == s) {
                    orderArray[i] = myCards.get(Tokens[i]).n;
                } else {
                    orderArray[i] = -100;
                }
            }
            orderArray[0] = orderArray[1] - 1;

            Log.d(TAG, "Tokens problem()1 - " + Arrays.toString(Tokens));


        } else if ((myCards.get(Tokens[0]).v != 0)) {
            String cards = "";
            for (int i = 0; i < Tokens.length; i++) {
                cards += " " + myCards.get(Tokens[i]).toString();
            }
            Log.d(TAG, "cards - " + cards);
            for (int i = 0; i < Tokens.length; i++) {
                Log.d(TAG, "Tokens problem()2 - " + Arrays.toString(Tokens));
                if ((myCards.get(Tokens[i]).n == 14 || myCards.get(Tokens[i]).n == 15)) {
                    orderArray[i] = myCards.get(Tokens[i - 1]).n + 1;
                } else if (myCards.get(Tokens[i]).s == s) {
                    orderArray[i] = myCards.get(Tokens[i]).n;
                } else {
                    orderArray[i] = -100;
                }
            }
        } else {
            Log.d(TAG, "Tokens problem()3 - " + Arrays.toString(Tokens));

            for (int i = Tokens.length - 1; i >= 0; i--) {
                Log.d(TAG, "getOrder() - i: " + i + " -  " + myCards.get(Tokens[i]).toString());

                if ((myCards.get(Tokens[i]).n == 14 || myCards.get(Tokens[i]).n == 15)) {
                    orderArray[i] = myCards.get(Tokens[i + 1]).n - 1;
                } else if (myCards.get(Tokens[i]).s == s) {
                    orderArray[i] = myCards.get(Tokens[i]).n;
                } else {
                    orderArray[i] = -100;
                }
            }
        }
        Log.d(TAG, "getOrder() - orderArray: " + Arrays.toString(orderArray));

        return orderArray;
    }

    private char getSymbol(int Tokens[]) {
        char sym = 'X';
        for (int i = 0; i < Tokens.length; i++) {
            if (myCards.get(Tokens[i]).v != 0) {
                return myCards.get(Tokens[i]).s;
            }
        }
        return sym;
    }

    private int[] tokensStringToInt(String[] Tokens) {
        int[] integerTokens = new int[Tokens.length];
        for (int i = 0; i < Tokens.length; i++) {
            try {
                integerTokens[i] = Integer.parseInt(Tokens[i]);
            } catch (NumberFormatException ex) { // handle your exception

                return invalidDrop;
            }
        }
        return integerTokens;
    }

    public void checkYanivOpportunity() {
        if (mySum <= yanivMinScore) {
            ((findViewById(R.id.yaniv_declare))).setVisibility(View.VISIBLE);
        } else {
            ((findViewById(R.id.yaniv_declare))).setVisibility(View.GONE);
        }
    }

    public void declareYanivOnClick(View view) {
        //send a declare message to all participants.
        byte[] sendMsg = new byte[5];
        sendMsg[0] = (int) 7;

        int asafCount = 0;
        int min = mySum;
        int thisGameScores[] = new int[mParticipants.size()];
        for(int i = 0; i < mParticipants.size(); i++){
            String pid = mParticipants.get(i).getParticipantId();
            thisGameScores[i] = getPlayerScore(mParticipants.get(i));
            if (pid.equals(mMyId)) {
                int yanivCallerIndex = i;
            } else {
                if (thisGameScores[i] <= min){
                    min = thisGameScores[i];
                }
            }
        }

        messageToAllParticipants(sendMsg, true);
        updateParticipantsCardsOnGameOverUI();
        // each participant "shows" his cards and his score UI

        //the game stop.
    }

    public String getMyCardsWithVal() {
        String ans = "";
        for (int i = 0; i < myCards.size(); i++) {
            ans += "" + i + "-[" + myCards.get(i) + "] ";
        }
        return ans;
    }

    public String getPopPrimaryDeckCardWithVal() {
        String ans = "";
        if (primaryDeck == null) {
            Log.d(TAG, "primaryDeck=null !!!!!!");

        } else {
            Log.d(TAG, "primaryDeck=" + primaryDeck.toString());

        }

        if (lastDropType == 1) {
            ans = "" + 0 + "-[" + (primaryDeck.peek().get((0)) + "] ");

        } else if (lastDropType == 2) {
            for (int i = 0; i < primaryDeck.peek().size(); i++) {
                ans += "" + i + "-[" + primaryDeck.peek().get(i) + "] ";
            }
        } else {
            ans += "" + 0 + "-[" + (primaryDeck.peek().get((0)) + "] ");
            ans += "" + (primaryDeck.peek().size() - 1) + "-[" + (primaryDeck.peek().get((primaryDeck.peek().size() - 1)) + "] ");
        }
        return ans;
    }

    public void initialCardsDrawable() {
        char hearts = '\u2764'; //hearts
        char spades = '\u2660'; //spades
        char diamonds = '\u2666'; //diamonds
        char clubs = '\u2663'; //clubs
        char joker = '\u2668'; //joker

        cardsDrawable.put("" + 1 + clubs, R.drawable.c_1_of_clubs); //clubs
        cardsDrawable.put("" + 1 + diamonds, R.drawable.c_1_of_diamonds); //diamonds
        cardsDrawable.put("" + 1 + hearts, R.drawable.c_1_of_hearts); //hearts
        cardsDrawable.put("" + 1 + spades, R.drawable.c_1_of_spades); //spades

        cardsDrawable.put("" + 2 + clubs, R.drawable.c_2_of_clubs); //clubs
        cardsDrawable.put("" + 2 + diamonds, R.drawable.c_2_of_diamonds); //diamonds
        cardsDrawable.put("" + 2 + hearts, R.drawable.c_2_of_hearts); //hearts
        cardsDrawable.put("" + 2 + spades, R.drawable.c_2_of_spades); //spades

        cardsDrawable.put("" + 3 + clubs, R.drawable.c_3_of_clubs); //clubs
        cardsDrawable.put("" + 3 + diamonds, R.drawable.c_3_of_diamonds); //diamonds
        cardsDrawable.put("" + 3 + hearts, R.drawable.c_3_of_hearts); //hearts
        cardsDrawable.put("" + 3 + spades, R.drawable.c_3_of_spades); //spades

        cardsDrawable.put("" + 4 + clubs, R.drawable.c_4_of_clubs); //clubs
        cardsDrawable.put("" + 4 + diamonds, R.drawable.c_4_of_diamonds); //diamonds
        cardsDrawable.put("" + 4 + hearts, R.drawable.c_4_of_hearts); //hearts
        cardsDrawable.put("" + 4 + spades, R.drawable.c_4_of_spades); //spades

        cardsDrawable.put("" + 5 + clubs, R.drawable.c_5_of_clubs); //clubs
        cardsDrawable.put("" + 5 + diamonds, R.drawable.c_5_of_diamonds); //diamonds
        cardsDrawable.put("" + 5 + hearts, R.drawable.c_5_of_hearts); //hearts
        cardsDrawable.put("" + 5 + spades, R.drawable.c_5_of_spades); //spades

        cardsDrawable.put("" + 6 + clubs, R.drawable.c_6_of_clubs); //clubs
        cardsDrawable.put("" + 6 + diamonds, R.drawable.c_6_of_diamonds); //diamonds
        cardsDrawable.put("" + 6 + hearts, R.drawable.c_6_of_hearts); //hearts
        cardsDrawable.put("" + 6 + spades, R.drawable.c_6_of_spades); //spades

        cardsDrawable.put("" + 7 + clubs, R.drawable.c_7_of_clubs); //clubs
        cardsDrawable.put("" + 7 + diamonds, R.drawable.c_7_of_diamonds); //diamonds
        cardsDrawable.put("" + 7 + hearts, R.drawable.c_7_of_hearts); //hearts
        cardsDrawable.put("" + 7 + spades, R.drawable.c_7_of_spades); //spades

        cardsDrawable.put("" + 8 + clubs, R.drawable.c_8_of_clubs); //clubs
        cardsDrawable.put("" + 8 + diamonds, R.drawable.c_8_of_diamonds); //diamonds
        cardsDrawable.put("" + 8 + hearts, R.drawable.c_8_of_hearts); //hearts
        cardsDrawable.put("" + 8 + spades, R.drawable.c_8_of_spades); //spades

        cardsDrawable.put("" + 9 + clubs, R.drawable.c_9_of_clubs); //clubs
        cardsDrawable.put("" + 9 + diamonds, R.drawable.c_9_of_diamonds); //diamonds
        cardsDrawable.put("" + 9 + hearts, R.drawable.c_9_of_hearts); //hearts
        cardsDrawable.put("" + 9 + spades, R.drawable.c_9_of_spades); //spades

        cardsDrawable.put("" + 10 + clubs, R.drawable.c_10_of_clubs); //clubs
        cardsDrawable.put("" + 10 + diamonds, R.drawable.c_10_of_diamonds); //diamonds
        cardsDrawable.put("" + 10 + hearts, R.drawable.c_10_of_hearts); //hearts
        cardsDrawable.put("" + 10 + spades, R.drawable.c_10_of_spades); //spades

        cardsDrawable.put("" + 11 + clubs, R.drawable.c_11_of_clubs); //clubs
        cardsDrawable.put("" + 11 + diamonds, R.drawable.c_11_of_diamonds); //diamonds
        cardsDrawable.put("" + 11 + hearts, R.drawable.c_11_of_hearts); //hearts
        cardsDrawable.put("" + 11 + spades, R.drawable.c_11_of_spades); //spades

        cardsDrawable.put("" + 12 + clubs, R.drawable.c_12_of_clubs); //clubs
        cardsDrawable.put("" + 12 + diamonds, R.drawable.c_12_of_diamonds); //diamonds
        cardsDrawable.put("" + 12 + hearts, R.drawable.c_12_of_hearts); //hearts
        cardsDrawable.put("" + 12 + spades, R.drawable.c_12_of_spades); //spades

        cardsDrawable.put("" + 13 + clubs, R.drawable.c_13_of_clubs); //clubs
        cardsDrawable.put("" + 13 + diamonds, R.drawable.c_13_of_diamonds); //diamonds
        cardsDrawable.put("" + 13 + hearts, R.drawable.c_13_of_hearts); //hearts
        cardsDrawable.put("" + 13 + spades, R.drawable.c_13_of_spades); //spades

        cardsDrawable.put("" + 14 + joker, R.drawable.c_0_of_black); //joker1
        cardsDrawable.put("" + 15 + joker, R.drawable.c_0_of_red); //joker2


    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void myCard1OnClick(View view) {

        Log.d(TAG, "myCard1OnClick=" + primaryDeck.toString());
        Drawable highlight = getResources().getDrawable(R.drawable.highlight);
        String card = myCards.get(0).toString();

        if (view.isSelected()) {
            view.setSelected(false);
            view.setBackground(null);
            myCardsDrop.remove((Integer) 0);
            myCardsDropToString.remove(card);
        } else {
            view.setSelected(true);
            view.setBackground(highlight);
            myCardsDrop.add(0);
            myCardsDropToString.add(card);
        }
        ((TextView) findViewById(R.id.my_drop)).setText(myCardsDropToString.toString());

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void myCard2OnClick(View view) {

        Log.d(TAG, "myCard2OnClick=" + primaryDeck.toString());
        Drawable highlight = getResources().getDrawable(R.drawable.highlight);
        String card = myCards.get(1).toString();

        if (view.isSelected()) {
            view.setSelected(false);
            view.setBackground(null);
            myCardsDrop.remove((Integer) 1);
            myCardsDropToString.remove(card);

        } else {
            view.setSelected(true);
            view.setBackground(highlight);
            myCardsDrop.add(1);
            myCardsDropToString.add(card);

        }
        ((TextView) findViewById(R.id.my_drop)).setText(myCardsDropToString.toString());

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void myCard3OnClick(View view) {

        Log.d(TAG, "myCard3OnClick=" + primaryDeck.toString());
        Drawable highlight = getResources().getDrawable(R.drawable.highlight);
        String card = myCards.get(2).toString();

        if (view.isSelected()) {
            view.setSelected(false);
            view.setBackground(null);
            myCardsDrop.remove((Integer) 2);
            myCardsDropToString.remove(card);

        } else {
            view.setSelected(true);
            view.setBackground(highlight);
            myCardsDrop.add(2);
            myCardsDropToString.add(card);

        }
        ((TextView) findViewById(R.id.my_drop)).setText(myCardsDropToString.toString());

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void myCard4OnClick(View view) {

        Log.d(TAG, "myCard4OnClick=" + primaryDeck.toString());
        Drawable highlight = getResources().getDrawable(R.drawable.highlight);
        String card = myCards.get(3).toString();

        if (view.isSelected()) {
            view.setSelected(false);
            view.setBackground(null);
            myCardsDrop.remove((Integer) 3);
            myCardsDropToString.remove(card);

        } else {
            view.setSelected(true);
            view.setBackground(highlight);
            myCardsDrop.add(3);
            myCardsDropToString.add(card);

        }
        ((TextView) findViewById(R.id.my_drop)).setText(myCardsDropToString.toString());

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void myCard5OnClick(View view) {

        Log.d(TAG, "myCard5OnClick=" + primaryDeck.toString());
        Drawable highlight = getResources().getDrawable(R.drawable.highlight);
        String card = myCards.get(4).toString();

        if (view.isSelected()) {
            view.setSelected(false);
            view.setBackground(null);
            myCardsDrop.remove((Integer) 4);
            myCardsDropToString.remove(card);

        } else {
            view.setSelected(true);
            view.setBackground(highlight);
            myCardsDrop.add(4);
            myCardsDropToString.add(card);

        }
        ((TextView) findViewById(R.id.my_drop)).setText(myCardsDropToString.toString());

    }

    private int getPlayerScore(Participant p){
        String id = p.getParticipantId();
        int sum = 0;
        for (Card card : mParticipantCards.get(id)) {
            sum += card.v;
        }
        return sum;
    }

    private void updateHighscores(){
        int i = 0;
        for (Participant p : mParticipants) {
            String id = p.getParticipantId();
            int sum = 0;
            for (Card card : mParticipantCards.get(id)) {
                sum += card.v;
            }
            highscores[i++] += sum;
        }
    }



    private String formatScoresToString(){
        String message = "";

        for (Participant p : mParticipants) {
            String name = p.getDisplayName();
            String id = p.getParticipantId();
            int sum = 0;
            for (Card card : mParticipantCards.get(id)) {
                sum += card.v;
            }
            message += name + ": " + sum +  System.getProperty("line.separator");
        }

        return message;
    }


    public void showHighscore(View view) {

        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Highscores")
                .setMessage(formatScoresToString())
                .show();

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void dropedCard1OnClick(View view) {
        takeFromDroppedCards(0);

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void dropedCard2OnClick(View view) {
        takeFromDroppedCards(1);

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void dropedCard3OnClick(View view) {
        takeFromDroppedCards(2);

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void dropedCard4OnClick(View view) {
        takeFromDroppedCards(3);

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void dropedCard5OnClick(View view) {
        takeFromDroppedCards(4);

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public boolean takeFromDroppedCards(int dropCard) {
        Log.d(TAG, "myCard1OnClick=");
        boolean b = checkValidDrop();
        if (b) {
            boolean b2 = checkValidTake(dropCard);
            if (b2) {
                final ArrayList<Card> myDrop = new ArrayList<Card>();

                for (int i = 0; i < myCardsDrop.size(); i++) {

                    int card = myCardsDrop.get(i);
                    myDrop.add(myCards.get(card));
                    Log.d(TAG, "Dropping: " + myCardsDrop.get(i) + " sym: " + myDrop.get(i));

                }
                for (int i = 0; i < myDrop.size(); i++) {
                    myCards.remove(myDrop.get(i));
                }

                Card pop;
                ArrayList<Card> lastDrop = primaryDeck.pop();
                pop = lastDrop.remove(dropCard);
                primaryDeck.add(lastDrop);
                myCards.add(pop);
                primaryDeck.add(myDrop);
                myCardsDrop.removeAllElements();
                myCardsDropToString.removeAllElements();
                ((TextView) findViewById(R.id.my_drop)).setText(myCardsDrop.toString());

                removeHighLightFromCards();


                scoreOnePoint();
                //take and drop
                return true;
            } else {
                // toat not valid taking
                return false;

            }

        } else {
            //toast not valid drop
            return false;

        }
    }

    private boolean checkValidTake(int dropCard) {
        Log.d(TAG, "takeFromLastDrop() called ");

        ArrayList<Card> lastDrop = primaryDeck.peek();

        //Taking card from 3 optional options to take.
        if (lastDropType == 1) {    //case one card - taking the only one card are in primary deck.
            if (dropCard >= 0 && dropCard <= lastDrop.size() - 1) {

            } else {

                Toast.makeText(this, "You select illegal cards to take, Try again.",
                        Toast.LENGTH_LONG).show();
                return false;
            }

        } else if (lastDropType == 2) { //case equal
            if (dropCard >= 0 && dropCard <= lastDrop.size() - 1) {

            } else {

                Toast.makeText(this, "You select illegal cards to take, Try again.",
                        Toast.LENGTH_LONG).show();
                return false;
            }
        } else {   //case order
            if (dropCard == 0 || dropCard == lastDrop.size() - 1) {

            } else {

                Toast.makeText(this, "You select illegal cards to take, Try again.",
                        Toast.LENGTH_LONG).show();
                return false;
            }


        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void cardDeckOnClick(View view) {
        Log.d(TAG, "cardDeckOnClick");
        boolean b = checkValidDrop();
        if (b) {
            final ArrayList<Card> myDrop = new ArrayList<Card>();

            for (int i = 0; i < myCardsDrop.size(); i++) {

                int card = myCardsDrop.get(i);
                myDrop.add(myCards.get(card));
                Log.d(TAG, "Dropping: " + myCardsDrop.get(i) + " sym: " + myDrop.get(i));

            }
            for (int i = 0; i < myDrop.size(); i++) {
                myCards.remove(myDrop.get(i));
            }

            Card pop = cardDeck.jp.remove(0);
            myCards.add(pop);
            primaryDeck.add(myDrop);
            myCardsDrop.removeAllElements();
            myCardsDropToString.removeAllElements();
            ((TextView) findViewById(R.id.my_drop)).setText(myCardsDrop.toString());

            removeHighLightFromCards();
            scoreOnePoint();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void removeHighLightFromCards() {

        Log.d(TAG, "removeHighLightFromCards()");
        ((ImageView) (findViewById(R.id.my_card_1))).setBackground(null);
        ((ImageView) (findViewById(R.id.my_card_2))).setBackground(null);
        ((ImageView) (findViewById(R.id.my_card_3))).setBackground(null);
        ((ImageView) (findViewById(R.id.my_card_4))).setBackground(null);
        ((ImageView) (findViewById(R.id.my_card_5))).setBackground(null);

    }

    private boolean checkValidDrop() {
        if (myCardsDrop == null || myCardsDrop.size() == 0) {

            Toast.makeText(this, "Select cards to drop, Try again. ",
                    Toast.LENGTH_LONG).show();
            return false;
        }
        int[] split = new int[myCardsDrop.size()];
        for (int i = 0; i < myCardsDrop.size(); i++) {
            split[i] = myCardsDrop.get(i);
        }
        int CVD = checkVaildDrop(split);
        if (CVD == 0) {

            Toast.makeText(this, "You enter illegal cards to drop, Try again. ",
                    Toast.LENGTH_LONG).show();
            return false;
        } else {


            this.myLastDropType = CVD;
            return true;
        }
    }


}
