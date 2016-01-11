package it.jaschke.alexandria;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;

import it.jaschke.alexandria.api.Callback;
import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.services.BookService;
import it.jaschke.alexandria.services.DownloadImage;


public class BookDetail extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String EAN_KEY = "EAN";
    private final int LOADER_ID = 10;
    private View rootView;
    private String ean;
    private String bookTitle;
    private ShareActionProvider shareActionProvider;
    private MenuItem menuItem;

    public static final String LOG_TAG = "BookDetail";

    public BookDetail(){
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
       // Log.v(LOG_TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        Bundle arguments = getArguments();

        if (arguments != null) {

            ean = arguments.getString(BookDetail.EAN_KEY);
        //    Log.v(LOG_TAG, "arguments present " + ean);
            getLoaderManager().restartLoader(LOADER_ID, null, this);
        }//else{
         //   Log.v(LOG_TAG, "no arguments present");
       // }
    }


    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //Log.v(LOG_TAG, "onCreateView");


        rootView = inflater.inflate(R.layout.fragment_full_book, container, false);
        rootView.findViewById(R.id.delete_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent bookIntent = new Intent(getActivity(), BookService.class);
                bookIntent.putExtra(BookService.EAN, ean);
                bookIntent.setAction(BookService.DELETE_BOOK);
                getActivity().startService(bookIntent);
                //getActivity().getSupportFragmentManager().popBackStack();

                /*
                When you delete the book, nothing happens, until you re-click the item.
                 */

               TextView title = (TextView) rootView.findViewById(R.id.fullBookTitle);
                title.setVisibility(View.GONE);
                ImageView bookCoverImage = (ImageView) rootView.findViewById(R.id.fullBookCover);
                bookCoverImage.setVisibility(View.GONE);


                TextView subTitle = (TextView) rootView.findViewById(R.id.fullBookSubTitle);
                subTitle.setVisibility(View.GONE);

                TextView bookDescription = (TextView) rootView.findViewById(R.id.fullBookDesc);
                bookDescription.setVisibility(View.GONE);

                TextView bookCategories = (TextView) rootView.findViewById(R.id.categories);
                bookCategories.setVisibility(View.GONE);

                TextView bookAuthors = (TextView) rootView.findViewById(R.id.authors);
                bookAuthors.setVisibility(View.GONE);

                Button bookDeleteButton = (Button) rootView.findViewById(R.id.delete_button);
                bookDeleteButton.setVisibility(View.GONE);

                TextView bookProcessingMessage = (TextView) rootView.findViewById(R.id.processing_message);
                bookProcessingMessage.setText(R.string.booked_deleted);
              //  Log.v(LOG_TAG, "DELETED BOOK");
                ((BookDetailCallback) getActivity()).bookDeleted();

            }
        });
        return rootView;
    }

    public interface BookDetailCallback{
        void bookDeleted();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
       // Log.v(LOG_TAG, "onCreateOptionsMenu");
        inflater.inflate(R.menu.book_detail, menu);

        this.menuItem = menu.findItem(R.id.action_share);
        //fixed the share intent, now it does not crash on screen rotation
        shareActionProvider = (android.support.v7.widget.ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text) + bookTitle);
        shareActionProvider.setShareIntent(shareIntent);

    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        //Log.v(LOG_TAG, "onCreateLoader " + ean.toString());
        return new CursorLoader(
                getActivity(),
                AlexandriaContract.BookEntry.buildFullBookUri(Long.parseLong(ean)),
                null,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
     //   Log.v(LOG_TAG, "onLoadFinished");
        if (!data.moveToFirst()) {
            return;
        }

        bookTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.TITLE));
        ((TextView) rootView.findViewById(R.id.fullBookTitle)).setText(bookTitle);




        String bookSubTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.SUBTITLE));
        ((TextView) rootView.findViewById(R.id.fullBookSubTitle)).setText(bookSubTitle);

        String desc = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.DESC));
        ((TextView) rootView.findViewById(R.id.fullBookDesc)).setText(desc);

        String authors = data.getString(data.getColumnIndex(AlexandriaContract.AuthorEntry.AUTHOR));
        String[] authorsArr = authors.split(",");
        ((TextView) rootView.findViewById(R.id.authors)).setLines(authorsArr.length);
        ((TextView) rootView.findViewById(R.id.authors)).setText(authors.replace(",","\n"));
        String imgUrl = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.IMAGE_URL));
        if(Patterns.WEB_URL.matcher(imgUrl).matches()){
            new DownloadImage((ImageView) rootView.findViewById(R.id.fullBookCover)).execute(imgUrl);
            rootView.findViewById(R.id.fullBookCover).setVisibility(View.VISIBLE);
        }

        String categories = data.getString(data.getColumnIndex(AlexandriaContract.CategoryEntry.CATEGORY));
        ((TextView) rootView.findViewById(R.id.categories)).setText(categories);

       /* if(rootView.findViewById(R.id.right_container)!=null){
            rootView.findViewById(R.id.backButton).setVisibility(View.INVISIBLE);
        }*/

    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {

    }

    @Override
    public void onPause() {
        super.onDestroyView();

      //  Log.v(LOG_TAG, "onPause");
        if(MainActivity.IS_TABLET && rootView.findViewById(R.id.right_container)==null){
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }
}