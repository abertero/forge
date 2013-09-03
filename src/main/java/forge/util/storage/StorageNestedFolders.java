package forge.util.storage;

import java.io.File;
import java.util.HashMap;

import org.apache.commons.lang.NotImplementedException;

import com.google.common.base.Function;


public class StorageNestedFolders<T> extends StorageBase<IStorage<T>> {

    private final File thisFolder;

    public StorageNestedFolders(File thisFolder, Iterable<File> subfolders, Function<File, IStorage<T>> factory) {
        super("<Subfolders>", new HashMap<String, IStorage<T>>());
        this.thisFolder = thisFolder;
        for(File sf : subfolders )
        {
            IStorage<T> newUnit = factory.apply(sf);
            map.put(sf.getName(), newUnit);
        }
    }

    // need code implementations for folder create/delete operations
    
    @Override
    public void add(IStorage<T> deck) {
        File subdir = new File(thisFolder, deck.getName());
        subdir.mkdir();
        
        // TODO: save recursivelly the passed IStorage 
        throw new NotImplementedException();
    }
    
    @Override
    public void delete(String deckName) {
        File subdir = new File(thisFolder, deckName);
        IStorage<T> f = map.remove(deckName);
        
        // TODO: Clear all that files from disk
        if ( f != null )
            subdir.delete(); // won't work if not empty;
    }
    
}