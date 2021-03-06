/*
 * Copyright (C) 2013 - 2017 Michiel van Loon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mfvl.trac.client;

import android.net.Uri;

import java.util.ArrayList;
import java.util.Map;

/**
 * Interface for the fragments to communicate with each other and the main activity
 *
 * @author Michiel
 */
interface InterFragmentListener {
    void enableDebug();

    boolean debugEnabled();

    void onChooserSelected(OnFileSelectedListener oc);

    void onTicketSelected(Ticket ticket);

    void onUpdateTicket(Ticket ticket);

    void refreshOverview();

    void startProgressBar(int resid);

    void stopProgressBar();

    TicketListAdapter getAdapter();

    void getTicket(int ticknr, OnTicketLoadedListener oc);

    void refreshTicket(int ticknr);

    TracClientService getService();

    void showAlertBox(final int titleres, final CharSequence message);

    int getNextTicket(int i);

    int getPrevTicket(int i);

    int getTicketCount();

    int getTicketContentCount();

    void updateTicket(Ticket t, String action, String comment, String veld, String waarde, final boolean notify, Map<String, String> modVeld) throws
            Exception;

    int createTicket(Ticket t, boolean notify) throws Exception;

    void listViewCreated();

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean isFinishing();

    void getAttachment(Ticket t, String filename, onAttachmentCompleteListener oc);

    void addAttachment(final Ticket ticket, final Uri uri, final onTicketCompleteListener oc);

    @SuppressWarnings("CollectionDeclaredAsConcreteClass")
    ArrayList<FilterSpec> parseFilterString(String filterString);

    @SuppressWarnings("CollectionDeclaredAsConcreteClass")
    ArrayList<SortSpec> parseSortString(String sortString);
}
