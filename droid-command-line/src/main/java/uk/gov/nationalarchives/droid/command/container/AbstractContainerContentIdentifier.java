/**
 * Copyright (c) 2012, The National Archives <pronom@nationalarchives.gsi.gov.uk>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of the The National Archives nor the
 *    names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package uk.gov.nationalarchives.droid.command.container;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import uk.gov.nationalarchives.droid.container.*;
import uk.gov.nationalarchives.droid.container.ole2.Ole2IdentifierEngine;
import uk.gov.nationalarchives.droid.container.zip.ZipIdentifierEngine;
import uk.gov.nationalarchives.droid.core.interfaces.IdentificationMethod;
import uk.gov.nationalarchives.droid.core.interfaces.IdentificationRequest;
import uk.gov.nationalarchives.droid.core.interfaces.IdentificationResultCollection;
import uk.gov.nationalarchives.droid.core.interfaces.IdentificationResultImpl;
import uk.gov.nationalarchives.droid.core.interfaces.resource.InputStreamIdentificationRequest;

/**
 *
 * @author rbrennan
 */
public abstract class AbstractContainerContentIdentifier implements ContainerContentIdentifier {

    private File tempDir;
    private ContainerIdentifierInit containerIdentifierInit;
    private IdentifierEngine identifierEngine;
    
    private Map<Integer, List<FileFormatMapping>> formats = new HashMap<Integer, List<FileFormatMapping>>(); 
    
    /**
     * Get the Identifier engine.
     * 
     * @return The identifier engine
     */
    public IdentifierEngine getIdentifierEngine() {
        return identifierEngine;
    }

    /**
     * @param identifierEngine The identifier engine
     */
    public void setIdentifierEngine(final IdentifierEngine identifierEngine) {
        this.identifierEngine = identifierEngine;
    }
    
    /**
     * @param defs The Container Signature Definitions
     * @param containerType The Container Type
     */
    @Override
    public void init(final ContainerSignatureDefinitions defs, final String containerType) {
        containerIdentifierInit = new ContainerIdentifierInit();
        containerIdentifierInit.init(defs, containerType, formats, null);
    }

    /**
     * @return The Container Identifier Initializer
     */
    public ContainerIdentifierInit getContainerIdentifierInit() {
        return containerIdentifierInit;
    }

    /**
     * @return The File Format mappings
     */
    public Map<Integer, List<FileFormatMapping>> getFormats() {
        return formats;
    }
    
    /**
     * @param in The input stream to identify
     * @param containerResults The results object to populate
     *
     * @return The identified results
     * 
     * @throws IOException If an error occurs with reading the input stream
     */
    @Override
    public IdentificationResultCollection process(
        final InputStream in, final IdentificationResultCollection containerResults) throws IOException {

        final IdentificationRequest<InputStream> request = new InputStreamIdentificationRequest(null, null, tempDir);

        try {
            request.open(in);
            
            int maxBytesToScan = -1;
            ContainerSignatureMatchCollection matches =
                new ContainerSignatureMatchCollection(getContainerIdentifierInit().getContainerSignatures(),
                    getContainerIdentifierInit().getUniqueFileEntries(), maxBytesToScan);
        
            getIdentifierEngine().process(request, matches);
        
            final Map<String, String> puidMap = new HashMap<String, String>();      
            for (ContainerSignatureMatch match : matches.getContainerSignatureMatches()) {
                if (match.isMatch()) {
                    List<FileFormatMapping> mappings = getFormats().get(match.getSignature().getId());
                    for (FileFormatMapping mapping : mappings) {
                        IdentificationResultImpl result = new IdentificationResultImpl();
                        result.setMethod(IdentificationMethod.CONTAINER);
                        result.setRequestMetaData(request.getRequestMetaData());
                        String puid = mapping.getPuid();
                        result.setPuid(mapping.getPuid());
                        if (!puidMap.containsKey(puid)) {
                            puidMap.put(puid, "");
                            containerResults.addResult(result);
                        }
                    }
                }
            }
            request.close();
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return containerResults;
    }

    @Override
    public void setTempDir(File tempDir) {
        this.tempDir = tempDir;
    }

    public static AbstractContainerContentIdentifier getContainerContentIdentifier(ContainerContentIdentifierType identifierType, ContainerSignatureDefinitions containerSignatureDefinitions, File tempDir) {

        //BNO: TODO: We currently return a new instance each time - e.g. each request from ArchiveContentIdentifier
        // will result in a new instance.  Could we have just a single instance of each subtype?  Only the temp
        // paths seem to be different...
        AbstractContainerContentIdentifier containerContentIdentifier = null;
        AbstractIdentifierEngine identifierEngine = null;

        try {
            switch(identifierType) {
                case OLE2:
                    containerContentIdentifier = new Ole2ContainerContentIdentifier();
                    identifierEngine  = new Ole2IdentifierEngine();
                    break;

                case ZIP:
                    containerContentIdentifier = new ZipContainerContentIdentifier();
                    identifierEngine = new ZipIdentifierEngine();
                    break;
                default:
                    throw new Exception("Unsupported container identifier type!");
            }
            containerContentIdentifier.init(containerSignatureDefinitions, identifierType.toString());

            containerContentIdentifier.setIdentifierEngine(identifierEngine);
            containerContentIdentifier.setTempDir(tempDir);
        } catch (Exception e) {
            //TODO - Log?
        }
        return containerContentIdentifier;

    }

    public enum ContainerContentIdentifierType {
        OLE2, ZIP
    }
}
