package com.example.newsreaderapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

	ArrayList <String> titles = new ArrayList<>();
	ArrayList <String> content = new ArrayList<>();

	ArrayAdapter arrayAdapter;

	SQLiteDatabase articlesDB;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		articlesDB = this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);
		articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleID INTEGER, title VARCHAR, content VARCHAR)");

		DownloadTask task = new DownloadTask();

		try {

			task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");

		} catch (Exception e) {
			e.printStackTrace();
		}

		ListView listView = findViewById(R.id.listView);
		arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, titles);
		listView.setAdapter(arrayAdapter);

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Intent intent = new Intent(getApplicationContext(),ArticleActivity.class);
				intent.putExtra("content",content.get(position));
				startActivity(intent);
			}
		});
		updateListView();
	}

	public void updateListView() {
		Cursor cursor = articlesDB.rawQuery("SELECT * FROM articles",null);

		int titleIndex = cursor.getColumnIndex("title");
		int contentIndex = cursor.getColumnIndex("content");

		if (cursor.moveToFirst()) {
			titles.clear();
			content.clear();

			do {

				titles.add(cursor.getString(titleIndex));
				content.add(cursor.getString(contentIndex));

			} while (cursor.moveToNext());

			arrayAdapter.notifyDataSetChanged();
		}
	}

	public class DownloadTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... urls) {

			String result = "";
			URL url;
			HttpURLConnection urlConnection = null;

			try {

				url = new URL (urls[0]);
				urlConnection = (HttpURLConnection) url.openConnection();
				InputStream inputStream = urlConnection.getInputStream();
				InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
				int data = inputStreamReader.read();

				while (data != -1) {
					char current = (char) data;
					result += current;
					data = inputStreamReader.read();
				}

				JSONArray jsonArray = new JSONArray(result);

				int numberOfItems = 100;
				if (jsonArray.length() < numberOfItems) {
					numberOfItems = jsonArray.length();
				}

				articlesDB.execSQL("DELETE FROM articles");

				for (int i = 0 ; i < numberOfItems; i++) {
					String articleID = jsonArray.getString(i);
					url = new URL("https://hacker-news.firebaseio.com/v0/item/" + articleID +".json?print=pretty");
					urlConnection = (HttpURLConnection) url.openConnection();
					inputStream = urlConnection.getInputStream();
					inputStreamReader = new InputStreamReader(inputStream);
					data = inputStreamReader.read();

					String articleInfo = "";

					while (data != -1) {
						char current = (char) data;
						articleInfo += current;
						data = inputStreamReader.read();
					}

					JSONObject jsonObject = new JSONObject(articleInfo);
					if (!jsonObject.isNull("title") && !jsonObject.isNull("url")) {
						String articleTitle = jsonObject.getString("title");
						String articleURL = jsonObject.getString("url");


						String sql = "INSERT INTO articles (articleID, title, content) VALUES (?, ?, ?)";
						SQLiteStatement statement = articlesDB.compileStatement(sql);
						statement.bindString(1 , articleID);
						statement.bindString(2 , articleTitle);
						statement.bindString(3 , articleURL);

						statement.execute();
					}
				}

				//Log.i("URL Content", result);
				return result;

			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;
		}

		@Override
		protected void onPostExecute(String s) {
			super.onPostExecute(s);

			updateListView();
		}
	}
}