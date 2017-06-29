package com.yaniv.online;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.games.multiplayer.Participant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;
import java.util.logging.Handler;

public class SinglePlayer extends Activity {
    boolean yaniv = false;

    private int numOfPC;
    ArrayList<PCPlayer> mParticipants = null;
    private int mMyId = 0;
    int yanivMinScore = 5;
    private int[] highscores;
    Vector<Integer> myCardsDrop = new Vector<>();
    Vector<String> myCardsDropToString = new Vector<>();
    final static String TAG = "[Yaniv-Single]";
    private int turn = 0;

    int cardsID[] = {R.id.my_card_1, R.id.my_card_2, R.id.my_card_3, R.id.my_card_4, R.id.my_card_5};
    int cardsTopID[] = {R.id.player_top_card_1, R.id.player_top_card_2, R.id.player_top_card_3, R.id.player_top_card_4, R.id.player_top_card_5};
    int cardsRightID[] = {R.id.player_right_card_1, R.id.player_right_card_2, R.id.player_right_card_3, R.id.player_right_card_4, R.id.player_right_card_5};
    int cardsLeftID[] = {R.id.player_left_card_1, R.id.player_left_card_2, R.id.player_left_card_3, R.id.player_left_card_4, R.id.player_left_card_5};
    int droppedID[] = {R.id.dropped_1, R.id.dropped_2, R.id.dropped_3, R.id.dropped_4, R.id.dropped_5};

    String whosTurn;

    //Participants common objects
    private Stack<ArrayList<Card>> primaryDeck;
    private Cards cardDeck;
    //Participant objects
    private Vector<Card> myCards;
    private int mySum;
    private int lastDropType = 1;
    private int myLastDropType;
    // private Vector<Vector<Card>> participantsCards = null;
    private int[] invalidDrop = {999};
    // Score of other participants. We update this as we receive their scores
    // from the network.
    Map<String, Integer> mParticipantScore = new HashMap<String, Integer>();
    Map<String, Vector<Card>> mParticipantCards = new HashMap<String, Vector<Card>>();
    Map<String, String> mParticipantPlayerPosition = new HashMap<String, String>();
    static Map<String, Integer> cardsDrawable = new HashMap<String, Integer>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getNumOfPC();
        initialCardsDrawable();

    }

    //First Initial
    void ownerInitial() {
        //((TextView) findViewById(R.id.owner)).setText("owner");
        this.myCards = new Vector<>();
        //highscores = new int[mParticipants.size()];
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
        //   intialParticipantAray();
        updateMyUI();
        updateParticipantsNamesAndUI();
        updatePrimaryDeckUI();
        // startGame();

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


    private void startGame() {
        Log.d(TAG, "startGame()");

        calculateSum();
        //int player = (int) (Math.random() * (mParticipants.size()+1));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        updatePeerScoresDisplay();
        for (int i = 0; i < mParticipants.size(); i++) {
            Log.d(TAG, "mParticipants()  " + mParticipants.get(i).getDisplayName() + " cards: " + mParticipants.get(i).getMyCards());

          //  mParticipants.get(Integer.parseInt(whosTurn)).play(primaryDeck, cardDeck);
            if (mParticipants.get(Integer.parseInt(whosTurn)).isYanivDeclare()) {
                yaniv = true;
            }
            turn = ++turn % (mParticipants.size() + 1);
            updatePrimaryDeckUI();
            updateParticipantUI(mParticipants.get(i).getParticipantId());

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1000); //wait 2 seconds
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //your 2nd command
                }
            }).start();
        }


    }

    private void createParticipantsCards() {
        Log.d(TAG, "createParticipantsCards() " + numOfPC);
        mParticipants = new ArrayList<>();
        String names[] = getNames();

        for (int j = 0; j < 5; j++) {
            myCards.add(cardDeck.jp.remove(0));
        }

        //   mParticipantCards.put("" + mMyId, myCards);

        for (int i = 0; i < numOfPC; i++) {
            Log.d(TAG, "createParticipantsCards() -1");
            Vector<Card> v = new Vector<>();
            for (int j = 0; j < 5; j++) {
                v.add(cardDeck.jp.remove(0));
            }
            String id = "" + (i + 1);

            mParticipantCards.put(id, v);
       //     PCPlayer p = new PCPlayer(names[i], "" + (i + 1), 0, yanivMinScore, v);


         //   mParticipants.add(p);


        }
        Log.d(TAG, "createParticipantsCards() -  mParticipantCards=" + mParticipantCards.toString());

        setPlayerPositonUI();

    }

    private void intialParticipantAray() {
        mParticipants = new ArrayList<>();
        String names[] = getNames();
        for (int i = 0; i < numOfPC; i++) {
            Log.d(TAG, "intialParticipantAray() -  i=" + i + "  -  cards=" + mParticipantCards.get(i).toString());

       //     mParticipants.add(new PCPlayer(names[i], "" + (i + 1), 0, yanivMinScore, mParticipantCards.get(i)));
        }
    }

    private void getNumOfPC() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Choose amount of players");
        String[] types = {"1", "2", "3"};
        b.setItems(types, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();
                switch (which) {
                    case 0:
                        numOfPC = 1;
                        intialParticipantAray();
                        ownerInitial();


                        break;
                    case 1:
                        numOfPC = 2;
                        intialParticipantAray();
                        ownerInitial();

                        break;
                    case 2:
                        numOfPC = 3;
                        intialParticipantAray();
                        ownerInitial();

                        break;
                }
            }

        });

        b.show();
    }

    private String[] getNames() {
        ArrayList<String> names = new ArrayList<>(Arrays.asList("David", "Rony", "Or", "Sapir", "Meni", "Yaron", "Elitzor", "Ron", "Tal", "Vered", "Yeuda", "Yael", "Matan"));
        String[] ans = new String[numOfPC];
        for (int i = 0; i < numOfPC; i++) {
            int rand = ((int) Math.random() * names.size());
            ans[i] = names.remove(rand);
        }
        return ans;
    }

    // This array lists all the individual screens our game has.
    final static int[] SCREENS = {
            R.id.screen_game, R.id.screen_main, R.id.screen_wait};
    int mCurScreen = -1;

    void switchToScreen(int screenId) {
        // make the requested screen visible; hide all others.
        for (int id : SCREENS) {
            findViewById(id).setVisibility(screenId == id ? View.VISIBLE : View.GONE);
        }
        mCurScreen = screenId;

        // should we show the invitation popup?
        boolean showInvPopup;

        // single-player: show on main screen and gameplay screen
        showInvPopup = (mCurScreen == R.id.screen_main || mCurScreen == R.id.screen_game);

    }

    void switchToMainScreen() {
        switchToScreen(R.id.screen_main);
    }

    private void setPlayerPositonUI() {
        Log.d(TAG, "setPlayerPositonUI()");
        for (int i = 0; i < mParticipants.size(); i++) {
            Log.d(TAG, "setPlayerPositonUI() - mParticipants.get(" + i + ")=" + mParticipants.get(i).getParticipantId());

        }
        boolean top = false;
        boolean left = false;
        for (int i = 0; i < numOfPC; i++) {

            if (!top) {
                mParticipantPlayerPosition.put("top", mParticipants.get(i).getParticipantId());
                Log.d(TAG, "setPlayerPositonUI() - top - " + mParticipants.get(i).getParticipantId() + " - " + mParticipants.get(i).getDisplayName());

                top = true;
            } else if (!left) {
                mParticipantPlayerPosition.put("left", mParticipants.get(i).getParticipantId());
                Log.d(TAG, "setPlayerPositonUI() - left - " + mParticipants.get(i).getParticipantId() + " - " + mParticipants.get(i).getDisplayName());

                left = true;
            } else {
                mParticipantPlayerPosition.put("right", mParticipants.get(i).getParticipantId());

            }
        }
        for (int i = 0; i < cardsTopID.length; i++) {
            (findViewById(cardsTopID[i])).setClickable(false);
            (findViewById(cardsLeftID[i])).setClickable(false);
            (findViewById(cardsRightID[i])).setClickable(false);
        }


    }

    public void updateParticipantsNamesAndUI() {
        Log.d(TAG, "updateParticipantsNames()");
        Log.d(TAG, "updateParticipantsNames() - mParticipantPlayerPosition - " + mParticipantPlayerPosition);
        Log.d(TAG, "updateParticipantsNames() - mParticipantPlayerPosition - " + mParticipantPlayerPosition.toString());


        for (int i = 0; i < mParticipants.size(); i++) {
            String id = mParticipants.get(i).getParticipantId();
            if (!id.equals(mMyId)) {
                String a = mParticipantPlayerPosition.get("top");
                Log.d(TAG, "updateParticipantsNames() - a - " + a);
                Log.d(TAG, "updateParticipantsNames() - pid - " + id);

                if (mParticipantPlayerPosition.get("top").equals(id)) {
                    ((TextView) findViewById(R.id.topName)).setText(mParticipants.get(i).getDisplayName());

                } else if (mParticipantPlayerPosition.get("left").equals(id)) {
                    ((TextView) findViewById(R.id.leftName)).setText(mParticipants.get(i).getDisplayName());
                } else {
                    ((TextView) findViewById(R.id.rightName)).setText(mParticipants.get(i).getDisplayName());

                }

            }
        }
     /*   //update all participant cards ui
        for (int i = 0; i < mParticipants.size(); i++) {
            String id = mParticipants.get(i).getParticipantId();
            if (!id.equals(mMyId)) {
                updateParticipantUI(id);
            }
        }*/
    }

    // updates the screen with the scores from our peers
    void updatePeerScoresDisplay() {
        Log.d(TAG, "updatePeerScoresDisplay() ");
        Log.d(TAG, "updatePeerScoresDisplay() - turn=" + turn + " mmyid=" + +mMyId);

        if (isTurn("" + mMyId)) {
            Log.d(TAG, "updatePeerScoresDisplay() - myturn");

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

            ((Button) findViewById(R.id.topPlayIcon)).setVisibility(View.GONE);
            ((Button) findViewById(R.id.rightPlayIcon)).setVisibility(View.GONE);
            ((Button) findViewById(R.id.leftPlayIcon)).setVisibility(View.GONE);
            ((Button) findViewById(R.id.myPlayIcon)).setVisibility(View.VISIBLE);
            whosTurn = "" + 0;
//            ((TextView)findViewById(R.id.rightName)).setTypeface(null, Typeface.NORMAL);
//            ((TextView)findViewById(R.id.leftName)).setTypeface(null, Typeface.NORMAL);
//            ((TextView)findViewById(R.id.topName)).setTypeface(null, Typeface.NORMAL);

        } else {
            Log.d(TAG, "updatePeerScoresDisplay() - not myturn");

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
                ((Button) findViewById(R.id.topPlayIcon)).setVisibility(View.VISIBLE);
                ((Button) findViewById(R.id.rightPlayIcon)).setVisibility(View.GONE);
                ((Button) findViewById(R.id.leftPlayIcon)).setVisibility(View.GONE);
                ((Button) findViewById(R.id.myPlayIcon)).setVisibility(View.GONE);
                whosTurn = "" + 1;
//                ((TextView)findViewById(R.id.topName)).setTypeface(null, Typeface.BOLD);
//                Log.d(TAG, "updateTurnUi: top: " +mParticipantPlayerPosition.get("top"));
//                ((TextView)findViewById(R.id.rightName)).setTypeface(null, Typeface.NORMAL);
//                ((TextView)findViewById(R.id.leftName)).setTypeface(null, Typeface.NORMAL);


            } else if (isTurn(mParticipantPlayerPosition.get("left"))) {
                ((Button) findViewById(R.id.topPlayIcon)).setVisibility(View.GONE);
                ((Button) findViewById(R.id.rightPlayIcon)).setVisibility(View.GONE);
                ((Button) findViewById(R.id.leftPlayIcon)).setVisibility(View.VISIBLE);
                ((Button) findViewById(R.id.myPlayIcon)).setVisibility(View.GONE);
                whosTurn = "" + 2;

//                ((TextView)findViewById(R.id.leftName)).setTypeface(null, Typeface.BOLD);
//                Log.d(TAG, "updateTurnUi: left: " +mParticipantPlayerPosition.get("left"));
//                ((TextView)findViewById(R.id.topName)).setTypeface(null, Typeface.NORMAL);
//                ((TextView)findViewById(R.id.rightName)).setTypeface(null, Typeface.NORMAL);


            } else {
                ((Button) findViewById(R.id.topPlayIcon)).setVisibility(View.GONE);
                ((Button) findViewById(R.id.rightPlayIcon)).setVisibility(View.VISIBLE);
                ((Button) findViewById(R.id.leftPlayIcon)).setVisibility(View.GONE);
                ((Button) findViewById(R.id.myPlayIcon)).setVisibility(View.GONE);
//                ((TextView)findViewById(R.id.rightName)).setTypeface(null, Typeface.BOLD);
//                Log.d(TAG, "updateTurnUi: right: " +mParticipantPlayerPosition.get("right"));
//                ((TextView)findViewById(R.id.topName)).setTypeface(null, Typeface.NORMAL);
//                ((TextView)findViewById(R.id.leftName)).setTypeface(null, Typeface.NORMAL);
                whosTurn = "" + 3;


            }
        }


    }

    public void updateMyUI() {
        Log.d(TAG, "updateMyUI() - mycards=" + myCards.toString());

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

    public void checkYanivOpportunity() {
        if (mySum <= yanivMinScore) {
            ((findViewById(R.id.yaniv_declare))).setVisibility(View.VISIBLE);
        } else {
            ((findViewById(R.id.yaniv_declare))).setVisibility(View.GONE);
        }
    }

    boolean isTurn(String participant) {
        Log.d(TAG, "turn=" + turn + "  myid=" + mMyId);
        if (participant.equals("" + 0)) {
            return true;
        } else {
            return false;
        }
     /*   if (!mParticipants.get(turn).getParticipantId().equals(participant)) {
            Log.d(TAG, "myTurn=false");

            return false;
        } else {
            Log.d(TAG, "myTurn=true");

            return true;
        }*/
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

    private int getPlayerScore(String id) {
        int sum = 0;
        for (Card card : mParticipantCards.get(id)) {
            sum += card.v;
        }
        return sum;
    }

    public void declareYanivOnClick(View view) {
        //send a declare message to all participants.
        byte[] sendMsg = new byte[5];
        sendMsg[0] = (int) 7;

        int asafCount = 0;
        int min = mySum;
        int thisGameScores[] = new int[mParticipants.size()];
        for (int i = 0; i < mParticipants.size(); i++) {
            String pid = mParticipants.get(i).getParticipantId();
            thisGameScores[i] = getPlayerScore(mParticipants.get(i).getParticipantId());
            if (pid.equals(mMyId)) {
                int yanivCallerIndex = i;
            } else {
                if (thisGameScores[i] <= min) {
                    min = thisGameScores[i];
                }
            }
        }

        // each participant "shows" his cards and his score UI

        //the game stop.
    }

    void calculateSum() {
        mySum = 0;
        for (Card card : myCards) {
            mySum += card.v;
        }
        ((Button) (findViewById(R.id.button_score))).setText("Score: " + mySum);

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

    public static void initialCardsDrawable() {
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

    public void showHighscore(View view) {

        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Highscores")
                .setMessage(formatScoresToString())
                .show();

    }

    private String formatScoresToString() {
        String message = "";

        for (PCPlayer p : mParticipants) {
            String name = p.getDisplayName();
            String id = p.getParticipantId();
            int sum = 0;
            for (Card card : mParticipantCards.get(id)) {
                sum += card.v;
            }
            message += name + ": " + sum + System.getProperty("line.separator");
        }

        return message;
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
                startGame();

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

    boolean updateTurnUi() {
    /*    Log.d(TAG, "updateTurnUi: mparti0: " + mParticipants.get(0).getParticipantId());
        Log.d(TAG, "updateTurnUi: mpart1i: " + mParticipants.get(1).getParticipantId());
        Log.d(TAG, "updateTurnUi: mpart0icards: " + mParticipantCards.get(mParticipants.get(0).getParticipantId()));
        Log.d(TAG, "updateTurnUi: mpart1icards: " + mParticipantCards.get(mParticipants.get(1).getParticipantId()));

        Log.d(TAG, "updateTurnUi: myid: " + mMyId);
        Log.d(TAG, "updateTurnUi: turn: " + turn);*/
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

    void scoreOnePoint() {
        if (!updateTurnUi()) {
            return;
        }

        calculateSum();
        updateMyUI();
        // updateScoreDisplay();
        //   updatePeerScoresDisplay();

        // broadcast our new score to our peers
        turn = ++turn % (mParticipants.size() + 1);

        ((findViewById(R.id.yaniv_declare))).setVisibility(View.GONE);
        ((Button) (findViewById(R.id.button_score))).setText("Score: " + mySum);
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
            startGame();
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
}
