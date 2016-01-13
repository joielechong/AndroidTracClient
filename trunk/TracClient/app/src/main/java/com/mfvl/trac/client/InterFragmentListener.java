/*
 * Copyright (C) 2015 Michiel van Loon
 *);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * Licensed under the Apache License, Version 2.0 (the "License"
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mfvl.trac.client;

import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.view.Menu;

import java.util.Map;

/**
 * Interface for the fragments to communicate with each other and the main activity
 *
 * @author Michiel
 */
public interface InterFragmentListener {
    void enableDebug();

    void onChooserSelected(OnFileSelectedListener oc);

    void onLogin(String url, String username, String password, boolean sslHack, boolean sslHostNameHack, String profile);

    void onTicketSelected(Ticket ticket);

    void onUpdateTicket(Ticket ticket);

    void refreshOverview();

    void startProgressBar(int resid);

    void stopProgressBar();

    TicketModel getTicketModel();

    TicketListAdapter getAdapter();

    Ticket getTicket(int ticknr);

    void refreshTicket(int ticknr);

    int getNextTicket(int i);

    int getPrevTicket(int i);

    int getTicketCount();

    int getTicketContentCount();

    void updateTicket(Ticket t, String action, String comment, String veld, String waarde, final boolean notify, Map<String, String> modVeld) throws Exception;

    int createTicket(Ticket t, boolean notify) throws Exception;

    void setActionProvider(Menu menu, int resid);

    Intent shareList();

    Intent shareTicket(final Ticket ticket);

    void listViewCreated();

    boolean isFinishing();

    Handler getHandler();

    boolean getCanWriteSD();

    void getAttachment(Ticket t, String filename, onAttachmentCompleteListener oc);

    void addAttachment(final Ticket ticket, final Uri uri, final onTicketCompleteListener oc);
}
