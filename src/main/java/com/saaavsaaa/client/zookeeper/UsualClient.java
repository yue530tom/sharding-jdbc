package com.saaavsaaa.client.zookeeper;
/**
 * Created by aaa on 18-4-18.
 */

import com.saaavsaaa.client.untils.Constants;
import com.saaavsaaa.client.untils.PathUtil;
import com.saaavsaaa.client.untils.StringUtil;
import org.apache.zookeeper.*;

import java.util.List;

/*
* cache
* todo Sequential
*/
public class UsualClient extends BaseClient {
    private final boolean watched = true; //false
    
    UsualClient(String servers, int sessionTimeoutMilliseconds) {
        super(servers, sessionTimeoutMilliseconds);
    }

    public void createNamespace() throws KeeperException, InterruptedException {
        if (checkExists(rootNode)){
            return;
        }
        zooKeeper.create(rootNode, new byte[0], authorities, CreateMode.PERSISTENT);
    }
    
    public void deleteNamespace() throws KeeperException, InterruptedException {
        zooKeeper.delete(rootNode, Constants.VERSION);
    }
    
    public String getDataString(final String key) throws KeeperException, InterruptedException {
        return new String(getData(key));
    }

    public byte[] getData(final String key) throws KeeperException, InterruptedException {
        return zooKeeper.getData(PathUtil.getRealPath(rootNode, key), watched, null);
    }
    
    public void getData(final String key, final AsyncCallback.DataCallback callback, final Object ctx) throws KeeperException, InterruptedException {
        zooKeeper.getData(PathUtil.getRealPath(rootNode, key), watched, callback, ctx);
    }
    
    public boolean checkExists(final String key) throws KeeperException, InterruptedException {
        return null != zooKeeper.exists(PathUtil.getRealPath(rootNode, key), watched);
    }
    
    public boolean checkExists(final String key, final Watcher watcher) throws KeeperException, InterruptedException {
        return null != zooKeeper.exists(PathUtil.getRealPath(rootNode, key), watcher);
    }
    
    public List<String> getChildren(final String key) throws KeeperException, InterruptedException {
        return zooKeeper.getChildren(PathUtil.getRealPath(rootNode, key), watched);
    }
    
    public void createCurrentOnly(final String key, final String value, final CreateMode createMode) throws KeeperException, InterruptedException {
        zooKeeper.create(PathUtil.getRealPath(rootNode, key), value.getBytes(StringUtil.UTF_8), authorities, createMode);
    }
    
    /*
    * closed beta
    */
    public void createAllNeedPath(final String key, final String value, final CreateMode createMode) throws KeeperException, InterruptedException {
        if (key.indexOf(PathUtil.PATH_SEPARATOR) < -1){
            this.createCurrentOnly(key, value, createMode);
            return;
        }
        Transaction transaction = zooKeeper.transaction();
        //todo sync cache
        List<String> nodes = PathUtil.getPathOrderNodes(rootNode, key);
        for (int i = 0; i < nodes.size(); i++) {
            // todo contrast cache
            if (checkExists(nodes.get(i))){
                System.out.println("exist:" + nodes.get(i));
                continue;
            }
            System.out.println("not exist:" + nodes.get(i));
            if (i == nodes.size() - 1){
                createCurrentOnly(nodes.get(i), value, createMode);
            } else {
                createCurrentOnly(nodes.get(i), Constants.NOTHING_VALUE, createMode);
            }
        }
        
        // todo org.apache.zookeeper.KeeperException$NodeExistsException: KeeperErrorCode = NodeExists
        transaction.commit();
    }
    
    public void update(final String key, final String value) throws KeeperException, InterruptedException {
        zooKeeper.setData(PathUtil.getRealPath(rootNode, key), value.getBytes(StringUtil.UTF_8), Constants.VERSION);
    }
    
    public void updateWithCheck(final String key, final String value) throws KeeperException, InterruptedException {
        String realPath = PathUtil.getRealPath(rootNode, key);
        zooKeeper.transaction().check(realPath, Constants.VERSION).setData(realPath, value.getBytes(StringUtil.UTF_8), Constants.VERSION).commit();
    }
    
    public void deleteOnlyCurrent(final String key) throws KeeperException, InterruptedException {
        zooKeeper.delete(PathUtil.getRealPath(rootNode, key), Constants.VERSION);
        System.out.println("delete : " + PathUtil.getRealPath(rootNode, key));
    }
    
    private void deleteOnlyCurrent(final String key, final Transaction transaction) throws KeeperException, InterruptedException {
        zooKeeper.delete(PathUtil.getRealPath(rootNode, key), Constants.VERSION);
    }
    
    public void deleteOnlyCurrent(final String key, final AsyncCallback.VoidCallback callback, final Object ctx) throws KeeperException, InterruptedException {
        zooKeeper.delete(PathUtil.getRealPath(rootNode, key), Constants.VERSION, callback, ctx);
    }
    
    public void deleteAllChildren(final String key) throws KeeperException, InterruptedException {
        String realPath = PathUtil.getRealPath(rootNode, key);
        try {
            this.deleteOnlyCurrent(realPath);
        }catch (KeeperException.NotEmptyException ee){
            List<String> children = this.getChildren(realPath);
            for (String child : children) {
                child = realPath + PathUtil.PATH_SEPARATOR + child;
                this.deleteAllChildren(child);
            }
            this.deleteOnlyCurrent(realPath);
        } catch (KeeperException.NoNodeException ee){
            System.out.println(ee.getMessage());
            return;
        }
    }
    
    /*
    * delete the current node with force and delete the super node whose only child node is current node recursively
    */
    public void deleteCurrentBranch(final String key) throws KeeperException, InterruptedException {
        String path = PathUtil.getRealPath(rootNode, key);
        this.deleteAllChildren(path);
        String superPath = path.substring(0, path.lastIndexOf(PathUtil.PATH_SEPARATOR));
        try {
            this.deleteRecursively(superPath);
        } catch (KeeperException.NotEmptyException ee){
            return;
        }
    }
    
    private void deleteRecursively(final String path) throws KeeperException, InterruptedException {
        int index = path.lastIndexOf(PathUtil.PATH_SEPARATOR);
        if (index < 0){
            return;
        }
        String superPath = path.substring(0, index);
        try {
            this.deleteOnlyCurrent(path);
            this.deleteRecursively(superPath);
        } catch (KeeperException.NotEmptyException ee){
            List<String> children = this.getChildren(path);
            children.forEach((c) -> System.out.println(path + " exist other children " + c));
            return;
        }
    }
}

