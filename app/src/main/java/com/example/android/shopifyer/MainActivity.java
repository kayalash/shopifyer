package com.example.android.shopifyer;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    String url_orders = "https://shopicruit.myshopify.com/admin/orders.json?page=1&access_token=c32313df0d0ef512ca64d5b336a0d7c6";
    double amount = 0;
    int bags = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        OrdersAsyncTask task = new OrdersAsyncTask();
        task.execute(url_orders);
    }

    private void displayAmount(String totalAmount) {
        TextView amountTextView = (TextView) findViewById(
                R.id.dollar_amount);
        amountTextView.setText(totalAmount);
    }

    private void displayBags(int totalBags) {
        TextView bagsTextView = (TextView) findViewById(
                R.id.bronze_bags);
        bagsTextView.setText(Integer.toString(totalBags));
    }

    private class OrdersAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... url) {
            String orders = connect();
            calculate(orders);
            return orders;
        }

        @Override
        protected void onPostExecute(String orders) {
            displayAmount(Double.toString(amount));
            displayBags(bags);
        }

        private String connect() {
            String jsonResponse = null;
            URL url = createUrl(url_orders);

            try {
                jsonResponse = makeHttpRequest(url);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Problem making the HTTP request.", e);
            }
            return jsonResponse;
        }

        private URL createUrl(String stringUrl) {
            URL url = null;
            try {
                url = new URL(stringUrl);
            } catch (MalformedURLException e) {
                Log.e(LOG_TAG, "Problem building the URL.", e);
            }
            return url;
        }

        private String makeHttpRequest(URL url) throws IOException {
            String jsonResponse = "";

            if (url == null) {
                return jsonResponse;
            }

            HttpURLConnection urlConnection = null;
            InputStream inputStream = null;
            try {
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setReadTimeout(10000);
                urlConnection.setConnectTimeout(15000);
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                if (urlConnection.getResponseCode() == 200) {
                    inputStream = urlConnection.getInputStream();
                    jsonResponse = readFromStream(inputStream);
                } else {
                    Log.e(LOG_TAG, "Error response code: " + urlConnection.getResponseCode());
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "Problem retrieving the order JSON results.", e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            }
            return jsonResponse;
        }

        private String readFromStream(InputStream inputStream) throws IOException {
            StringBuilder output = new StringBuilder();
            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
                BufferedReader reader = new BufferedReader(inputStreamReader);
                String line = reader.readLine();
                while (line != null) {
                    output.append(line);
                    line = reader.readLine();
                }
            }
            return output.toString();
        }

        private void calculate(String orders) {
            try {
                JSONObject baseJsonResponse = new JSONObject(orders);
                JSONArray ordersArray = baseJsonResponse.getJSONArray("orders");

                for (int i = 0; i < ordersArray.length(); i++) {
                    JSONObject currentOrder = ordersArray.getJSONObject(i);

                    // Part 1
                    String currentEmail = currentOrder.getString("email");
                    if (currentEmail.equals("napoleon.batz@gmail.com")) {
                        String totalPriceString = currentOrder.getString("total_price");
                        double totalPrice = Double.parseDouble(totalPriceString);
                        amount += totalPrice;
                    }

                    // Part 2
                    JSONArray itemsArray = currentOrder.getJSONArray("line_items");
                    for (int j = 0; j < itemsArray.length(); j++) {
                        JSONObject currentItem = itemsArray.getJSONObject(j);
                        String itemName = currentItem.getString("title");
                        if (itemName.equals("Awesome Bronze Bag")) {
                            int quantity = currentItem.getInt("quantity");
                            bags += quantity;
                        }
                    }
                }
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Problem parsing the orders JSON results", e);
            }

        }
    }
}
