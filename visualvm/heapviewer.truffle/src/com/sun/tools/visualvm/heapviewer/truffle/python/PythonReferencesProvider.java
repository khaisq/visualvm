/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.tools.visualvm.heapviewer.truffle.python;

import com.sun.tools.visualvm.heapviewer.HeapFragment;
import com.sun.tools.visualvm.heapviewer.model.DataType;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNodeFilter;
import com.sun.tools.visualvm.heapviewer.model.Progress;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleObject;
import com.sun.tools.visualvm.heapviewer.ui.UIThresholds;
import com.sun.tools.visualvm.heapviewer.utils.NodesComputer;
import com.sun.tools.visualvm.heapviewer.utils.ProgressIterator;
import java.util.Iterator;
import java.util.List;
import javax.swing.SortOrder;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.HeapProgress;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.Value;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tomas Hurka
 */
@NbBundle.Messages({
    "PythonReferencesProvider_References=Computing references..."
})
@ServiceProvider(service = HeapViewerNode.Provider.class, position = 500)
public class PythonReferencesProvider extends HeapViewerNode.Provider {

    public String getName() {
        return "references";
    }

    public boolean supportsView(Heap heap, String viewID) {
        return viewID.startsWith("python_");
    }

    public boolean supportsNode(HeapViewerNode parent, Heap heap, String viewID) {
        return parent instanceof PythonNodes.PythonNode && !(parent instanceof PythonNodes.PythonObjectFieldNode);
    }

    public HeapViewerNode[] getNodes(HeapViewerNode parent, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
        return getNodes(getReferences(parent, heap), parent, heap, viewID, dataTypes, sortOrders, progress);
    }

    static HeapViewerNode[] getNodes(List<FieldValue> references, HeapViewerNode parent, final Heap heap, String viewID, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
        if (references == null) {
            return null;
        }

        NodesComputer<Integer> computer = new NodesComputer<Integer>(references.size(), UIThresholds.MAX_INSTANCE_REFERENCES) {
            protected boolean sorts(DataType dataType) {
                return !DataType.COUNT.equals(dataType);
            }

            protected HeapViewerNode createNode(Integer index) {
                FieldValue field = references.get(index);
                Instance instance = field.getDefiningInstance();
                
                // TODO: references are broken!!!
                if (PythonObject.isPythonObject(instance)) {
                    PythonObject robj = new PythonObject(instance);
                    return new PythonNodes.PythonObjectReferenceNode(robj, robj.getType(heap), field);
                } else {
                    List<Value> references = (List<Value>)instance.getReferences();
                    for (Value reference : references) {
                        instance = reference.getDefiningInstance();
                        if (PythonObject.isPythonObject(instance)) {
                            PythonObject robj = new PythonObject(instance);
                            return new PythonNodes.PythonObjectAttributeReferenceNode(robj, robj.getType(heap), field);
                        }
                    }
                }
                
                throw new IllegalArgumentException("Illegal reference " + field);
            }

            protected ProgressIterator<Integer> objectsIterator(int index, Progress progress) {
                Iterator<Integer> iterator = integerIterator(index, references.size());
                return new ProgressIterator(iterator, index, false, progress);
            }

            protected String getMoreNodesString(String moreNodesCount) {
                return "<another " + moreNodesCount + " references left>";
            }

            protected String getSamplesContainerString(String objectsCount) {
                return "<sample " + objectsCount + " references>";
            }

            protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx) {
                return "<references " + firstNodeIdx + "-" + lastNodeIdx + ">";
            }
        };

        return computer.computeNodes(parent, heap, viewID, null, dataTypes, sortOrders, progress);
    }

    private List<FieldValue> getReferences(HeapViewerNode parent, Heap heap) {
        TruffleObject object = parent == null ? null : HeapViewerNode.getValue(parent, TruffleObject.DATA_TYPE, heap);
        PythonObject pyobject = object instanceof PythonObject ? (PythonObject)object : null;
        if (pyobject == null) return null;
        
        ProgressHandle pHandle = null;

        try {
            pHandle = ProgressHandle.createHandle(Bundle.PythonReferencesProvider_References());
            pHandle.setInitialDelay(1000);
            pHandle.start(HeapProgress.PROGRESS_MAX);

            HeapFragment.setProgress(pHandle, 0);
            return pyobject.getReferences();
        } finally {
            if (pHandle != null) {
                pHandle.finish();
            }
        }

    }
}