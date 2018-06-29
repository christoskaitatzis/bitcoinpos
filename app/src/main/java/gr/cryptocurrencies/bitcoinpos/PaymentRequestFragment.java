package gr.cryptocurrencies.bitcoinpos;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import gr.cryptocurrencies.bitcoinpos.database.PointOfSaleDb;
import gr.cryptocurrencies.bitcoinpos.network.Requests;
import gr.cryptocurrencies.bitcoinpos.utilities.BitcoinUtils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Timer;
import java.util.TimerTask;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link PaymentRequestFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link PaymentRequestFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PaymentRequestFragment extends DialogFragment  {

    private static final String ARG_BITCOIN_ADDRESS = "bitcoinAddress";
    private static final String ARG_MERCHANT_NAME = "merchantName";
    private static final String ARG_PRIMARY_AMOUNT = "primaryAmount";
    private static final String ARG_SECONDARY_AMOUNT = "secondaryAmount";
    private static final String ARG_BITCOIN_IS_PRIMARY = "bitcoinIsPrimary";
    private static final String ARG_LOCAL_CURRENCY = "localCurrency";
    private static final String ARG_EXCHANGE_RATE = "exchangeRate";
    private static final String ARG_ITEMS_NAMES = "itemsNames";

    private String mBitcoinAddress;
    private String mMerchantName;
    private String mPrimaryAmount;
    private String mSecondaryAmount;
    private boolean mBitcoinIsPrimary;
    private String mLocalCurrency;
    private String mExchangeRate;
    private String mItemsNames;

    private ImageView mQrCodeImage;
    private Bitmap mQrCodeBitmap;

    private TextView mMerchantNameTextView;
    private TextView mBitcoinAddressTextView;
    private TextView mPrimaryAmountTextView;
    private TextView mSecondaryAmountTextView;
    private TextView mPaymentTextView;
    private TextView mPaymentStatusTextView;

    private Button mCancelButton;

    private Timer mTimer;
    private String mPaymentTransaction = null;

    private PointOfSaleDb mDbHelper;

    private OnFragmentInteractionListener mListener;

    public PaymentRequestFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param bitcoinAddress Parameter 1.
     * @param merchantName Parameter 2.
     * @param primaryAmount
     * @param secondaryAmount
     * @param bitcoinIsPrimary
     * @param localCurrency
     * @param exchangeRate
     * @param itemsNames
     * @return A new instance of fragment PaymentRequestFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PaymentRequestFragment newInstance(String bitcoinAddress, String merchantName, String primaryAmount, String secondaryAmount, boolean bitcoinIsPrimary, String localCurrency, String exchangeRate, String itemsNames) {
        PaymentRequestFragment fragment = new PaymentRequestFragment();
        Bundle args = new Bundle();
        args.putString(ARG_BITCOIN_ADDRESS, bitcoinAddress);
        args.putString(ARG_MERCHANT_NAME, merchantName);
        args.putString(ARG_PRIMARY_AMOUNT, primaryAmount);
        args.putString(ARG_SECONDARY_AMOUNT, secondaryAmount);
        args.putBoolean(ARG_BITCOIN_IS_PRIMARY, bitcoinIsPrimary);
        args.putString(ARG_LOCAL_CURRENCY, localCurrency);
        args.putString(ARG_EXCHANGE_RATE, exchangeRate);
        args.putString(ARG_ITEMS_NAMES, itemsNames);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mBitcoinAddress = getArguments().getString(ARG_BITCOIN_ADDRESS);
            mMerchantName = getArguments().getString(ARG_MERCHANT_NAME);
            mPrimaryAmount = getArguments().getString(ARG_PRIMARY_AMOUNT);
            mSecondaryAmount = getArguments().getString(ARG_SECONDARY_AMOUNT);
            mBitcoinIsPrimary = getArguments().getBoolean(ARG_BITCOIN_IS_PRIMARY);
            mLocalCurrency = getArguments().getString(ARG_LOCAL_CURRENCY);
            mExchangeRate = getArguments().getString(ARG_EXCHANGE_RATE);
            mItemsNames = getArguments().getString(ARG_ITEMS_NAMES);

            //Find screen size
            WindowManager manager = (WindowManager) getActivity().getSystemService(getContext().WINDOW_SERVICE);
            Display display = manager.getDefaultDisplay();
            Point point = new Point();
            display.getSize(point);
            int width = point.x;
            int height = point.y;
            int smallerDimension = width < height ? width : height;
            smallerDimension = smallerDimension * 3/4;

            // generate QR code to show in image view
            try {
                // generate BIP 21 compatible payment URI
                String bip21Payment = "bitcoin:" + mBitcoinAddress +
                        "?amount=" + (mBitcoinIsPrimary ? mPrimaryAmount : mSecondaryAmount) +
                        "&label=" + URLEncoder.encode(mMerchantName, "UTF-8");
                mQrCodeBitmap = encodeAsBitmap(bip21Payment, smallerDimension); //(mBitcoinAddress);
            } catch (WriterException we) {
                we.printStackTrace();
            } catch (UnsupportedEncodingException uee) {
                uee.printStackTrace();
            }

            
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View fragmentView = inflater.inflate(R.layout.fragment_payment_request, container, false);

        mMerchantNameTextView = (TextView) fragmentView.findViewById(R.id.merchant_name);
        mMerchantNameTextView.setText(mMerchantName);
        mMerchantNameTextView.setTextColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
        mMerchantNameTextView.setTextSize(getResources().getDimension(R.dimen.button_text_size_large));

        mPaymentTextView = (TextView) fragmentView.findViewById(R.id.payment_text);
        mPaymentTextView.setTextColor(ContextCompat.getColor(getContext(), android.R.color.black));
        mPaymentTextView.setTextSize(getResources().getDimension(R.dimen.button_text_size_normal));

        mPaymentStatusTextView = (TextView) fragmentView.findViewById(R.id.payment_status);
        mPaymentStatusTextView.setTextColor(ContextCompat.getColor(getContext(), android.R.color.holo_red_dark));
        mPaymentStatusTextView.setTextSize(getResources().getDimension(R.dimen.button_text_size_normal));

        mBitcoinAddressTextView = (TextView) fragmentView.findViewById(R.id.bitcoin_address);
        mBitcoinAddressTextView.setTextColor(ContextCompat.getColor(getContext(), android.R.color.black));
        mBitcoinAddressTextView.setTextSize(getResources().getDimension(R.dimen.button_text_size_small));
        mBitcoinAddressTextView.setText(mBitcoinAddress);

        // TODO put "BTC" in strings.xml

        String primaryAmountToDisplay, secondaryAmountToDisplay;
        if (mBitcoinIsPrimary) {
            primaryAmountToDisplay = mPrimaryAmount + " BTC";
            secondaryAmountToDisplay = "(" + mSecondaryAmount + " " + mLocalCurrency + ")";
        } else {
            primaryAmountToDisplay = mPrimaryAmount + " " + mLocalCurrency;
            secondaryAmountToDisplay = "(" + mSecondaryAmount + " BTC)";
        }

        mPrimaryAmountTextView = (TextView) fragmentView.findViewById(R.id.primary_amount);
        mPrimaryAmountTextView.setTextColor(ContextCompat.getColor(getContext(), android.R.color.black));
        mPrimaryAmountTextView.setText(primaryAmountToDisplay);

        mSecondaryAmountTextView = (TextView) fragmentView.findViewById(R.id.secondary_amount);
        mSecondaryAmountTextView.setText(secondaryAmountToDisplay);

        mCancelButton = (Button) fragmentView.findViewById(R.id.cancel_payment_button);
        mCancelButton.setText(getString(R.string.cancel));
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // call parents method to close dialog fragment
                mListener.onPaymentRequestFragmentClose(true);
            }
        });

        mQrCodeImage = (ImageView) fragmentView.findViewById(R.id.qr_code_image);
        mQrCodeImage.setImageBitmap(mQrCodeBitmap);

        return fragmentView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        // get DB helper
        mDbHelper = PointOfSaleDb.getInstance(getContext());

        // setup timer to check network for unconfirmed/confirmed transactions
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                double amount;
                if (mBitcoinIsPrimary) {
                    amount = Double.parseDouble(mPrimaryAmount);
                } else {
                    amount = Double.parseDouble(mSecondaryAmount);
                }

                if (mPaymentTransaction == null) {
                    Log.d("CHECK FOR UNCONFIRMED", amount + "!");
                    updateIfUnconfirmedTxId(mBitcoinAddress, amount);
                } else {
                    Log.d("CHECK FOR CONFIRMED", mPaymentTransaction);
                    updateIfTxConfirmed(mPaymentTransaction, mBitcoinAddress, amount);
                }
            }
        }, 0, 3000);

        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // stop network request timer
        if(mTimer != null)
            mTimer.cancel();

        mListener = null;
    }



    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onPaymentRequestFragmentClose(boolean isCancelled);
    }




    Bitmap encodeAsBitmap(String str, int size) throws WriterException {
        BitMatrix result;
        try {
            result = new MultiFormatWriter().encode(str, BarcodeFormat.QR_CODE, size, size);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            return null;
        }
        int width = result.getWidth();
        int height = result.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = result.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

//    Bitmap encodeAsBitmap(String str) throws WriterException {
//        String contentsToEncode = str;
//        if (contentsToEncode == null) {
//            return null;
//        }
//        Map<EncodeHintType,Object> hints = null;
//        String encoding = guessAppropriateEncoding(contentsToEncode);
//        if (encoding != null) {
//            hints = new EnumMap<>(EncodeHintType.class);
//            hints.put(EncodeHintType.CHARACTER_SET, encoding);
//        }
//        BitMatrix result;
//        try {
//            result = new MultiFormatWriter().encode(contentsToEncode, BarcodeFormat.QR_CODE, 240, 240, hints);
//        } catch (IllegalArgumentException iae) {
//            // Unsupported format
//            return null;
//        }
//        int width = result.getWidth();
//        int height = result.getHeight();
//        int[] pixels = new int[width * height];
//        for (int y = 0; y < height; y++) {
//            int offset = y * width;
//            for (int x = 0; x < width; x++) {
//                pixels[offset + x] = result.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
//            }
//        }
//
//        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
//        return bitmap;
//    }
//
//    private static String guessAppropriateEncoding(CharSequence contents) {
//        // Very crude at the moment
//        for (int i = 0; i < contents.length(); i++) {
//            if (contents.charAt(i) > 0xFF) {
//                return "UTF-8";
//            }
//        }
//        return null;
//    }


    // TODO what if we have > 1 tx on the exact amount the last 2 minutes?
    // get recent tx status of specific amount on specific bitcoin address
    private void updateIfUnconfirmedTxId(final String bitcoinAddress, final double amount) {
        String url;
        final double amountInSatoshis = amount * 100000000;

        if(BitcoinUtils.isMainNet()) {
            url = "https://blockchain.info/unconfirmed-transactions?format=json";
        } else {
            url = "https://testnet.blockchain.info/unconfirmed-transactions?format=json";
        }

        // TODO BLOCKCHAIN.INFO JSON attributes should be captured into static vars to avoid manual repetition
        // https://www.blockchain.com/api/
        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("UNCONFIRMED TX ID", response.toString());
                        try {
                            if(response.has("txs")) {
                                // get all unconfirmed transactions for this amount on that address (if it exists)
                                JSONArray unconfirmedTxs = response.getJSONArray("txs");
                                if(unconfirmedTxs.length() == 0) {
                                    // do nothing, TX not visible in the network
                                } else {
                                    // if found mark as Ongoing, otherwise do nothing (TODO is it possible to go to confirmed so fast it never appears here!?!
                                    for(int i=0; i<unconfirmedTxs.length(); i++) {
                                        JSONObject txObj = (JSONObject) unconfirmedTxs.get(i);
                                        JSONArray outArray = (JSONArray) txObj.getJSONArray("out");
                                        for(int outIndex=0; outIndex<outArray.length(); outIndex++) {
                                            JSONObject output = (JSONObject) outArray.get(outIndex);
                                            Log.d("UNCONFIRMED AMOUNTS", output.getDouble("value") + "=" + amountInSatoshis);
                                            if(output.getString("addr").equals(bitcoinAddress) && output.getDouble("value") == amountInSatoshis) {

                                                // if found remember the transaction id and use only this for confirmations
                                                mPaymentTransaction = txObj.getString("hash");
                                                mPaymentStatusTextView.setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
                                                mPaymentStatusTextView.setText(R.string.payment_unconfirmed);

                                                // update cancel button and listener
                                                mCancelButton.setText(R.string.ok);
                                                mCancelButton.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {
                                                        // stop timer that would check if transaction was confirmed
                                                        mTimer.cancel();

                                                        // call parents method to close dialog fragment
                                                        mListener.onPaymentRequestFragmentClose(false);
                                                    }
                                                });

                                                // payment is now visible to the network / write to transaction history
                                                String createdAt = txObj.getString("time"); // time is UNIX Epoch

                                                double btcAmount, localAmount;
                                                if(mBitcoinIsPrimary) {
                                                    btcAmount = Double.parseDouble(mPrimaryAmount);
                                                    localAmount = Double.parseDouble(mSecondaryAmount);
                                                } else {
                                                    btcAmount = Double.parseDouble(mSecondaryAmount);
                                                    localAmount = Double.parseDouble(mPrimaryAmount);
                                                }

                                                addTransaction(mPaymentTransaction, btcAmount, localAmount, mLocalCurrency,
                                                        createdAt, null, mMerchantName, mBitcoinAddress, false,
                                                        mItemsNames, mExchangeRate);

                                            }
                                        }

                                    }
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("UNCONFIRMED TX ID ERROR", error.toString());
                    }
                });

        Requests.getInstance(getContext()).addToRequestQueue(jsObjRequest);

    }


    // TODO USED from both here and HistoryFragment ... move to another class that represents Blockchain.Info API !!!
    // TODO note that for Blockchain.Info API https://testnet.blockchain.info/rawaddr/2MsYhJvb2ecBKRYT3UG7jLNVmV6eHLVpwRG
    // TODO could be used to check for both unconfirmed and confirmed txs
    // check if unconfirmed transaction was confirmed (uses mPaymentTransaction stored from getUnconfirmedTxId)
    private void updateIfTxConfirmed(final String tx, final String bitcoinAddress, final double amount) {
        String url;
        if(BitcoinUtils.isMainNet()) {
            url = "https://blockchain.info/rawaddr/" + bitcoinAddress;
        } else {
            url = "https://testnet.blockchain.info/rawaddr/" + bitcoinAddress;
        }

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("CONFIRMED TX", response.toString());
                        try {
                            if(response.has("address") && response.getString("address").equals(bitcoinAddress)) {

                                JSONArray allTxs = (JSONArray) response.getJSONArray("txs");
                                JSONObject ongoingTx;
                                for(int i=0; i<allTxs.length(); i++) {
                                    JSONObject trx = (JSONObject) allTxs.get(i);
                                    if(trx.getString("hash").equals(tx) && trx.has("block_height")) {
                                        // Transaction was confirmed - stop the timer/repetitions
                                        if(mTimer != null)
                                            mTimer.cancel();

                                        // if found transaction was confirmed
                                        mPaymentStatusTextView.setTextColor(ContextCompat.getColor(getContext(), R.color.brightGreen));
                                        mPaymentStatusTextView.setText(R.string.payment_confirmed);

                                        // transaction was confirmed / update transaction history
                                        String confirmedAt = trx.getString("time"); // time is unix timestamp
                                        updateDbTransactionToConfirmed(tx, confirmedAt);

                                        break;
                                    }
                                }

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("CONFIRMED TX ERROR", error.toString());
                    }
                });

        Requests.getInstance(getContext()).addToRequestQueue(jsObjRequest);

    }


    private void addTransaction(String txId, double btcAmount,
                                double localAmount, String localCurrency,
                                String createdAt, String confirmedAt,
                                String merchantName, String bitcoinAddress,
                                boolean isConfirmed, String itemsNames,
                                String exchangeRate) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // make sure transaction entry does not already exist:
        String[] tableColumns = { PointOfSaleDb.TRANSACTIONS_COLUMN_TX_ID };
        String whereClause = PointOfSaleDb.TRANSACTIONS_COLUMN_TX_ID + " = ?";
        String[] whereArgs = { txId };
        Cursor c = db.query(PointOfSaleDb.TRANSACTIONS_TABLE_NAME, tableColumns, whereClause, whereArgs, null, null, null);

        // if no row found with that txId add the transaction
        if(c != null && c.getCount() == 0) {
            ContentValues values = new ContentValues();
            values.put(PointOfSaleDb.TRANSACTIONS_COLUMN_TX_ID, txId);
            values.put(PointOfSaleDb.TRANSACTIONS_COLUMN_BITCOIN_AMOUNT, btcAmount);
            values.put(PointOfSaleDb.TRANSACTIONS_COLUMN_LOCAL_AMOUNT, localAmount);
            values.put(PointOfSaleDb.TRANSACTIONS_COLUMN_LOCAL_CURRENCY, localCurrency);
            values.put(PointOfSaleDb.TRANSACTIONS_COLUMN_CREATED_AT, createdAt);
            values.put(PointOfSaleDb.TRANSACTIONS_COLUMN_CONFIRMED_AT, confirmedAt);
            values.put(PointOfSaleDb.TRANSACTIONS_COLUMN_MERCHANT_NAME, merchantName);
            values.put(PointOfSaleDb.TRANSACTIONS_COLUMN_BITCOIN_ADDRESS, bitcoinAddress);
            values.put(PointOfSaleDb.TRANSACTIONS_COLUMN_IS_CONFIRMED, isConfirmed);
            values.put(PointOfSaleDb.TRANSACTIONS_COLUMN_PRODUCT_NAME, itemsNames);
            values.put(PointOfSaleDb.TRANSACTIONS_COLUMN_EXCHANGE_RATE, exchangeRate);
            db.insert(PointOfSaleDb.TRANSACTIONS_TABLE_NAME, null, values);
        }
    }

    private void updateDbTransactionToConfirmed(String txId, String confirmedAt) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(PointOfSaleDb.TRANSACTIONS_COLUMN_IS_CONFIRMED, true);
        values.put(PointOfSaleDb.TRANSACTIONS_COLUMN_CONFIRMED_AT, confirmedAt);

        // row to update
        String selection = PointOfSaleDb.TRANSACTIONS_COLUMN_TX_ID + " LIKE ?";
        String[] selectioinArgs = {String.valueOf(txId) };

        int count = db.update(PointOfSaleDb.TRANSACTIONS_TABLE_NAME, values, selection, selectioinArgs);
    }


}
