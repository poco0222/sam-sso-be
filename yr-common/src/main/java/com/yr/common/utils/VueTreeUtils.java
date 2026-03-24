package com.yr.common.utils;

import com.alibaba.fastjson.JSON;
import com.yr.common.core.domain.ITreeEntity;
import com.yr.common.core.domain.ObjectTree;
import org.apache.commons.compress.utils.Lists;

import java.util.List;

/**
 * <p>
 * description
 * </p>
 *
 * @author carl 2022-01-06 14:24
 * @version V1.0
 */
public class VueTreeUtils {

    public static List<ObjectTree> transform(List<? extends ITreeEntity> list) {
        List<ObjectTree> treeList = null;
        if (null != list) {
            treeList = Lists.newArrayList();
            for (ITreeEntity rootEntity : list) {
                //找到根目录
                if (null == rootEntity || rootEntity.getParentId().longValue() <= 0) {
                    ObjectTree rootTreeEntity = new ObjectTree(rootEntity);
                    //查找子类
                    rootTreeEntity.getChildren().addAll(transform(list, rootEntity.getId()));
                    //添加节点
                    treeList.add(rootTreeEntity);
                }
            }
        }
        return treeList;
    }

    /**
     * 找子节点
     *
     * @param list
     * @return
     */
    private static List<ObjectTree> transform(List<? extends ITreeEntity> list, Long parentId) {

        List<ObjectTree> childNodeList = Lists.newArrayList();

        for (ITreeEntity childNodeEntity : list) {
            if (parentId.longValue() == childNodeEntity.getParentId().longValue()) {

                ObjectTree objectTree = new ObjectTree(childNodeEntity);
                //添加子节点
                childNodeList.add(objectTree);
//                //继续找子节点
                objectTree.getChildren().addAll(transform(list, objectTree.getId()));
            }
        }
        return childNodeList;
    }

    public static void main(String[] args) {
//        List<TestTree> testList = new ArrayList<TestTree>(){
//            {
//                add(new TestTree(1L,0L,"A"));
//
//            }
//        };
        List<TestTree> testTrees = Lists.newArrayList();
        testTrees.add(new TestTree(1L, 0L, "A"));
        testTrees.add(new TestTree(2L, 0L, "B"));
        testTrees.add(new TestTree(3L, 0L, "C"));
        testTrees.add(new TestTree(4L, 1L, "A1"));
        testTrees.add(new TestTree(5L, 1L, "A2"));
        testTrees.add(new TestTree(6L, 4L, "A11"));
        testTrees.add(new TestTree(7L, 6L, "A111"));
        testTrees.add(new TestTree(8L, 2L, "B1"));
        testTrees.add(new TestTree(9L, 8L, "B11"));
        testTrees.add(new TestTree(10L, 3L, "C1"));
        List<ObjectTree> treeList = transform(testTrees);
        System.out.println(JSON.toJSONString(treeList));
    }

    public static class TestTree implements ITreeEntity {
        private Long id;
        private Long parentId;
        private String label;

        public TestTree() {
        }

        public TestTree(Long id, Long parentId, String label) {
            this.id = id;
            this.label = label;
            this.parentId = parentId;
        }

        @Override
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        @Override
        public Long getParentId() {
            return parentId;
        }

        public void setParentId(Long parentId) {
            this.parentId = parentId;
        }

        @Override
        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }
}
