/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */

package org.graalvm.visualvm.lib.ui.memory;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;
import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import org.graalvm.visualvm.lib.jfluid.results.ExportDataDumper;
import org.graalvm.visualvm.lib.jfluid.results.memory.SampledMemoryResultsDiff;
import org.graalvm.visualvm.lib.jfluid.results.memory.SampledMemoryResultsSnapshot;
import org.graalvm.visualvm.lib.ui.components.table.ClassNameTableCellRenderer;
import org.graalvm.visualvm.lib.ui.components.table.CustomBarCellRenderer;
import org.graalvm.visualvm.lib.ui.components.table.DiffBarCellRenderer;
import org.graalvm.visualvm.lib.ui.components.table.LabelTableCellRenderer;
import org.graalvm.visualvm.lib.profiler.api.GoToSource;


/**
 * This panel displays memory allocations diff.
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
public class DiffSampledResultsPanel extends SnapshotSampledResultsPanel {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.graalvm.visualvm.lib.ui.memory.Bundle"); // NOI18N
    private static final String GO_SOURCE_POPUP_ITEM_NAME = messages.getString("AllocResultsPanel_GoSourcePopupItemName"); // NOI18N
                                                                                                                           // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private SampledMemoryResultsDiff diff;
    private JMenuItem popupShowSource;
    private JPopupMenu memoryResPopupMenu;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public DiffSampledResultsPanel(SampledMemoryResultsSnapshot snapshot, MemoryResUserActionsHandler actionsHandler) {
        super(snapshot, actionsHandler);
        diff = (SampledMemoryResultsDiff) snapshot;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == popupShowSource && popupShowSource != null) {
            performDefaultAction(-1);
        }
    }

    public void exportData(int typeOfFile, ExportDataDumper eDD, String viewName) {
        switch (typeOfFile) {
            case 1: exportCSV(",", eDD); break;  //NOI18N
            case 2: exportCSV(";", eDD); break;  //NOI18N
            case 3: exportXML(eDD, viewName); break;
            case 4: exportHTML(eDD, viewName); break;
        }
    }

    private void exportHTML(ExportDataDumper eDD, String viewName) {
         // Header
        StringBuffer result = new StringBuffer("<HTML><HEAD><meta http-equiv=\"Content-type\" content=\"text/html; charset=utf-8\" /><TITLE>"+viewName+"</TITLE></HEAD><BODY><TABLE border=\"1\"><tr>"); // NOI18N
        for (int i = 0; i < (columnNames.length); i++) {
            if (!(columnRenderers[i]==null)) {
                result.append("<th>").append(columnNames[i]).append("</th>"); //NOI18N
            }
        }
        result.append("</tr>"); //NOI18N
        eDD.dumpData(result);

        for (int i=0; i < nTrackedItems; i++) {

            result = new StringBuffer("<tr><td>"+replaceHTMLCharacters(sortedClassNames[i])+"</td>"); //NOI18N
            result.append("<td align=\"right\">").append(totalLiveObjectsSize[i]).append("</td>"); //NOI18N
            result.append("<td align=\"right\">").append(nTotalLiveObjects[i]).append("</td></tr>"); //NOI18N
            eDD.dumpData(result);
        }
        eDD.dumpDataAndClose(new StringBuffer(" </TABLE></BODY></HTML>")); //NOI18N
    }

    private void exportXML(ExportDataDumper eDD, String viewName) {
         // Header
        String newline = System.getProperty("line.separator"); // NOI18N
        StringBuffer result = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+newline); // NOI18N
        result.append("<ExportedView Name=\"").append(viewName).append("\">").append(newline); //NOI18N
        result.append(" <TableData NumRows=\"").append(nTrackedItems).append("\" NumColumns=\"3\">").append(newline); //NOI18N
        result.append("<TableHeader>"); //NOI18N
        for (int i = 0; i < (columnNames.length); i++) {
            if (!(columnRenderers[i]==null)) {
                result.append("  <TableColumn><![CDATA[").append(columnNames[i]).append("]]></TableColumn>").append(newline); //NOI18N
            }
        }
        result.append("</TableHeader>"); //NOI18N
        eDD.dumpData(result);

        // Data
        for (int i=0; i < nTrackedItems; i++) {
            result = new StringBuffer("  <TableRow>"+newline+"   <TableColumn><![CDATA["+sortedClassNames[i]+"]]></TableColumn>"+newline); //NOI18N
            result.append("   <TableColumn><![CDATA[").append(totalLiveObjectsSize[i]).append("]]></TableColumn>").append(newline); //NOI18N
            result.append("   <TableColumn><![CDATA[").append(nTotalLiveObjects[i]).append("]]></TableColumn>").append(newline).append("  </TableRow>").append(newline); //NOI18N
            eDD.dumpData(result);
        }
        eDD.dumpDataAndClose(new StringBuffer(" </TableData>"+newline+"</ExportedView>")); //NOI18N
    }

    private void exportCSV(String separator, ExportDataDumper eDD) {
        // Header
        StringBuffer result = new StringBuffer();
        String newLine = "\r\n"; // NOI18N
        String quote = "\""; // NOI18N

        for (int i = 0; i < columnNames.length; i++) {
            if (!(columnRenderers[i]==null)) {
                result.append(quote).append(columnNames[i]).append(quote).append(separator);
            }
        }
        result.deleteCharAt(result.length()-1);
        result.append(newLine);
        eDD.dumpData(result);

        // Data
        for (int i=0; i < nTrackedItems; i++) {
            result = new StringBuffer();
            result.append(quote).append(sortedClassNames[i]).append(quote).append(separator);
            result.append(quote).append(totalLiveObjectsSize[i]).append(quote).append(separator);
            result.append(quote).append(nTotalLiveObjects[i]).append(quote).append(newLine);
            eDD.dumpData(result);
        }
        eDD.close();
    }

    private String replaceHTMLCharacters(String s) {
        StringBuilder sb = new StringBuilder();
        int len = s.length();
        for (int i = 0; i < len; i++) {
          char c = s.charAt(i);
          switch (c) {
              case '<': sb.append("&lt;"); break; //NOI18N
              case '>': sb.append("&gt;"); break; //NOI18N
              case '&': sb.append("&amp;"); break; //NOI18N
              case '"': sb.append("&quot;"); break; //NOI18N
              default: sb.append(c); break;
          }
        }
        return sb.toString();
    }

    protected CustomBarCellRenderer getBarCellRenderer() {
        return new DiffBarCellRenderer(diff.getMinObjectsSizePerClassDiff(), diff.getMaxObjectsSizePerClassDiff());
    }

    protected JPopupMenu getPopupMenu() {
        if (memoryResPopupMenu == null) {
            memoryResPopupMenu = new JPopupMenu();

            if (GoToSource.isAvailable()) {
                Font boldfont = memoryResPopupMenu.getFont().deriveFont(Font.BOLD);

                popupShowSource = new JMenuItem();
                popupShowSource.setFont(boldfont);
                popupShowSource.setText(GO_SOURCE_POPUP_ITEM_NAME);
                memoryResPopupMenu.add(popupShowSource);
                popupShowSource.addActionListener(this);
            }
        }

        return memoryResPopupMenu;
    }

    protected Object computeValueAt(int row, int col) {
        int index = ((Integer) filteredToFullIndexes.get(row)).intValue();

        switch (col) {
            case 0:
                return sortedClassNames[index];
            case 1:
                return new Long(totalLiveObjectsSize[index]);
            case 2:
                return ((totalLiveObjectsSize[index] > 0) ? "+" : "") + intFormat.format(totalLiveObjectsSize[index]) + " B"; // NOI18N
            case 3:
                return ((nTotalLiveObjects[index] > 0) ? "+" : "") + intFormat.format(nTotalLiveObjects[index]); // NOI18N
            default:
                return null;
        }
    }

    protected void initColumnsData() {
        super.initColumnsData();

        ClassNameTableCellRenderer classNameTableCellRenderer = new ClassNameTableCellRenderer();
        LabelTableCellRenderer labelTableCellRenderer = new LabelTableCellRenderer(JLabel.TRAILING);

        columnRenderers = new TableCellRenderer[] { classNameTableCellRenderer, null, labelTableCellRenderer, labelTableCellRenderer };
    }

    protected void initDataUponResultsFetch() {
        super.initDataUponResultsFetch();

        if (barRenderer != null) {
            barRenderer.setMinimum(diff.getMinObjectsSizePerClassDiff());
            barRenderer.setMaximum(diff.getMaxObjectsSizePerClassDiff());
        }
    }

    protected boolean passesValueFilter(int i) {
        return true;
    }

    protected void performDefaultAction(int classId) {
        String className = null;
        int selectedRow = resTable.getSelectedRow();

        if (selectedRow != -1) {
            className = (String) resTable.getValueAt(selectedRow, 0).toString().replace("[]", ""); // NOI18N;
        }

        if (className != null) {
            actionsHandler.showSourceForMethod(className, null, null);
        }
    }

    protected boolean truncateZeroItems() {
        return false;
    }
}
