package org.jboss.sbomer.sbom.service.core.utility;

import com.github.f4b6a3.tsid.TsidCreator;

// TODO to consider during deployment
/**
 * <p>
 * <strong>WARNING</strong>: make sure that you have set following environment
 * variables in production to avoid clashes:
 *
 * <ul>
 * <li>{@code TSIDCREATOR_NODE} - set it to a unique value per instance</li>
 * <li>{@code TSIDCREATOR_NODE_COUNT} - set it to the total number of
 * instances</li>
 * </ul>
 * </p>
 *
 * across services
 *
 * @see <a href="https://github.com/f4b6a3/tsid-creator/?tab=readme-ov-file#node-identifier">Node Identifier</a>
 */
public class TsidUtility {

    /**
     * Utility method to create a generation ID using Tsid, starting with G
     *
     * @return Tsid of generation
     */
    public static String createUniqueGenerationId() {
        return "G" + TsidCreator.getTsid1024().toString();
    }

    /**
     * Utility method to create a generation request ID using Tsid, starting with R
     *
     * @return Tsid of generation request
     */
    public static String createUniqueGenerationRequestId() {
        return "R" + TsidCreator.getTsid1024().toString();
    }

    /**
     * Utility method to create a enhancement ID using Tsid, starting with E
     *
     * @return Tsid of enhancement
     */
    public static String createUniqueEnhancementId() {
        return "E" + TsidCreator.getTsid1024().toString();
    }

    /**
     * Utility method to create a run ID using Tsid, starting with RUN
     *
     * @return Tsid of run
     */
    public static String createUniqueRunId() {
        return "RUN" + TsidCreator.getTsid1024().toString();
    }

}
