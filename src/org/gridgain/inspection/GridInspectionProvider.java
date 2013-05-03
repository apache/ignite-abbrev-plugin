// @java.file.header

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.inspection;

import com.intellij.codeInspection.*;
import org.gridgain.inspection.abbrev.*;
import org.gridgain.inspection.comment.*;

/**
 * Provider for inspections.
 *
 * @author @java.author
 * @version @java.version
 */
public class GridInspectionProvider implements InspectionToolProvider {
    public Class[] getInspectionClasses() {
        return new Class[] { GridAbbreviationInspection.class, GridCommentInspection.class };
    }
}
