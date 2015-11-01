package uk.gov.nationalarchives.droid.submitter;

import java.io.File;
import java.net.URI;

/**
 * Created by Brian on 31/10/2015.
 */
public class FileWithUri extends File {

    private URI uri;

    public FileWithUri(String pathname) {
        super(pathname);
    }

    public FileWithUri(String parent, String child) {
        super(parent, child);
    }

    public FileWithUri(File parent, String child) {
        super(parent, child);
    }

    public FileWithUri(URI uri) {
        super(uri);
        this.uri = uri;
    }


    public URI getUri() {
        if(this.uri == null) {
            this.uri = super.toURI();
        }
        return this.uri;
    }

    @Override
    public URI toURI() {
        return getUri();
    }
}
