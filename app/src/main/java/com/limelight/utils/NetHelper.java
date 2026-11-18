package com.limelight.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

public class NetHelper {
    public static boolean isActiveNetworkVpn(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network activeNetwork = connMgr.getActiveNetwork();
            if (activeNetwork != null) {
                NetworkCapabilities netCaps = connMgr.getNetworkCapabilities(activeNetwork);
                if (netCaps != null) {
                    return netCaps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ||
                            !netCaps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN);
                }
            }
        }
        else {
            NetworkInfo activeNetworkInfo = connMgr.getActiveNetworkInfo();
            if (activeNetworkInfo != null) {
                return activeNetworkInfo.getType() == ConnectivityManager.TYPE_VPN;
            }
        }

        return false;
    }

    public static int getIntelligentBitrate(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            if (capabilities != null) {
                int linkSpeedKbps = capabilities.getLinkDownstreamBandwidthKbps();
                if (linkSpeedKbps > 0) {
                    // Formula para Bitrate Inteligente:
                    // Recomendamos usar 80% da banda disponível para evitar stuttering.
                    // Limitamos entre 2 Mbps e 50 Mbps para estabilidade no mobile.
                    int recommended = (int) (linkSpeedKbps * 0.8);
                    if (recommended > 50000) recommended = 50000;
                    if (recommended < 2000) recommended = 2000;
                    return recommended;
                }
            }
        }
        // Fallback para 20 Mbps se não for possível determinar a velocidade
        return 20000;
    }
}
