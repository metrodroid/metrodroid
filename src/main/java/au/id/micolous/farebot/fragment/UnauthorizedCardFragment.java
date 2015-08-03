package au.id.micolous.farebot.fragment;


import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.simpleframework.xml.Serializer;

import au.id.micolous.farebot.FareBotApplication;
import au.id.micolous.farebot.R;
import au.id.micolous.farebot.activity.AdvancedCardInfoActivity;
import au.id.micolous.farebot.activity.CardInfoActivity;
import au.id.micolous.farebot.card.Card;
import au.id.micolous.farebot.transit.TransitData;


public class UnauthorizedCardFragment extends Fragment {
    private Card mCard;
    private TransitData mTransitData;

    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Serializer serializer = FareBotApplication.getInstance().getSerializer();
        mCard        = Card.fromXml(serializer, getArguments().getString(AdvancedCardInfoActivity.EXTRA_CARD));
        mTransitData = getArguments().getParcelable(CardInfoActivity.EXTRA_TRANSIT_DATA);
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_unauthorized_card, container, false);
        return view;
    }
}
