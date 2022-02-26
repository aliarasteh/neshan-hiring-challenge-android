package org.neshan.main;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.neshan.R;
import org.neshan.data.model.enums.RoutingType;
import org.neshan.data.model.response.Leg;
import org.neshan.databinding.BottomsheetLocationDetailBinding;

public class LocationDetailBottomSheet extends BottomSheetDialogFragment {

    private BottomsheetLocationDetailBinding mBinding;

    private MainActivityViewModel mSharedViewModel;

    // trigger action when bottom sheet closes
    private DialogInterface.OnDismissListener mOnDismissListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // set bottom sheet theme
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme);

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        mBinding = BottomsheetLocationDetailBinding.inflate(getLayoutInflater(), container, false);

        return mBinding.getRoot();

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Dialog dialog = getDialog();
        if (dialog != null) {
            // avoid default dim background for bottom sheet
            dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

            // when bottom sheet closed -> trigger dismiss listener
            dialog.setOnDismissListener(d -> {
                this.dismiss();
                if (mOnDismissListener != null) mOnDismissListener.onDismiss(d);
            });
        }

        mSharedViewModel = new ViewModelProvider(requireActivity()).get(MainActivityViewModel.class);
        observeViewModelChange(mSharedViewModel);
        // bind data to view
        mBinding.setViewModel(mSharedViewModel);

        setViewListeners();

    }

    public void setOnDismissListener(DialogInterface.OnDismissListener listener) {
        this.mOnDismissListener = listener;
    }

    private void setViewListeners() {

        mBinding.route.setOnClickListener(view1 -> {
            if (getDialog() != null) {
//                getDialog().dismiss();
            }
        });

    }

    private void observeViewModelChange(MainActivityViewModel viewModel) {

        // load direction (here by default for car)
        viewModel.loadDirection(RoutingType.CAR);

        viewModel.getRoutingDetailLiveData().observe(getViewLifecycleOwner(), routingDetail -> {
            if (routingDetail != null) {
                mBinding.route.setClickable(true);
                mBinding.route.setBackgroundResource(R.drawable.bg_radius_primary_25);
                try {
                    Leg leg = routingDetail.getRoutes().get(0).getLegs().get(0);
                    mBinding.distance.setText(leg.getDistance().getText());
                    mBinding.duration.setText(leg.getDuration().getText());
                } catch (NullPointerException exception) {
                    // failure in getting distance and duration
                    exception.printStackTrace();
                }
            }
        });

    }

}
