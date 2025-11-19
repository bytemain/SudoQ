package de.sudoq.model.actionTree;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import de.sudoq.model.sudoku.Cell;

public class BranchManagerTests {

    private BranchManager branchManager;
    private ActionTree actionTree;
    private ActionFactory factory;
    private Cell cell;

    @Before
    public void setUp() {
        branchManager = new BranchManager();
        actionTree = new ActionTree();
        factory = new SolveActionFactory();
        cell = new Cell(-1, 9);

        // Initialize with root
        branchManager.initialize(actionTree.getRoot());
    }

    @Test
    public void testInitialization() {
        assertNotNull(branchManager.getCurrentBranch());
        assertEquals(ActionBranch.Companion.getMAIN_BRANCH_ID(),
                branchManager.getCurrentBranch().getId());
        assertNotNull(branchManager.getCurrentNode());
        assertEquals(actionTree.getRoot().getId(),
                branchManager.getCurrentNode().getId());
    }

    @Test
    public void testCreateBranch() {
        // Add some actions
        ActionTreeElement node1 = actionTree.add(factory.createAction(5, cell), actionTree.getRoot());

        // Create a new branch
        ActionBranch newBranch = branchManager.createBranch("test-branch", node1, false);

        assertNotNull(newBranch);
        assertEquals("test-branch", newBranch.getName());
        assertEquals(node1.getId(), newBranch.getHead().getId());

        // Verify branch was added
        assertTrue(branchManager.getAllBranches().size() >= 2); // main + new branch
    }

    @Test
    public void testSwitchBranch() {
        // Create a branch
        ActionTreeElement node1 = actionTree.add(factory.createAction(5, cell), actionTree.getRoot());
        ActionBranch newBranch = branchManager.createBranch("test-branch", node1, false);

        // Switch to it
        branchManager.switchToBranch(newBranch);

        assertEquals(newBranch.getId(), branchManager.getCurrentBranch().getId());
        assertEquals(node1.getId(), branchManager.getCurrentNode().getId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCannotDeleteMainBranch() {
        branchManager.removeBranch(ActionBranch.Companion.getMAIN_BRANCH_ID());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCannotDeleteCurrentBranch() {
        ActionTreeElement node1 = actionTree.add(factory.createAction(5, cell), actionTree.getRoot());
        ActionBranch newBranch = branchManager.createBranch("test-branch", node1, false);
        branchManager.switchToBranch(newBranch);

        // Try to delete current branch - should throw
        branchManager.removeBranch(newBranch.getId());
    }

    @Test
    public void testDeleteBranch() {
        // Create and switch away from branch
        ActionTreeElement node1 = actionTree.add(factory.createAction(5, cell), actionTree.getRoot());
        ActionBranch newBranch = branchManager.createBranch("test-branch", node1, false);

        int initialCount = branchManager.getAllBranches().size();

        // Delete the branch (we're still on main)
        branchManager.removeBranch(newBranch.getId());

        assertEquals(initialCount - 1, branchManager.getAllBranches().size());
        assertNull(branchManager.getBranch(newBranch.getId()));
    }

    @Test
    public void testUpdateBranchHead() {
        ActionTreeElement node1 = actionTree.add(factory.createAction(5, cell), actionTree.getRoot());
        ActionTreeElement node2 = actionTree.add(factory.createAction(3, cell), node1);

        // Update main branch head to node2
        branchManager.updateBranchHead(ActionBranch.Companion.getMAIN_BRANCH_ID(), node2);

        ActionBranch mainBranch = branchManager.getBranch(ActionBranch.Companion.getMAIN_BRANCH_ID());
        assertEquals(node2.getId(), mainBranch.getHead().getId());
    }

    @Test
    public void testFindBranchByHead() {
        ActionTreeElement node1 = actionTree.add(factory.createAction(5, cell), actionTree.getRoot());
        branchManager.updateCurrentBranchHead(node1);

        ActionBranch found = branchManager.findBranchByHead(node1);
        assertNotNull(found);
        assertEquals(ActionBranch.Companion.getMAIN_BRANCH_ID(), found.getId());
    }

    @Test
    public void testViewMode() {
        ActionTreeElement node1 = actionTree.add(factory.createAction(5, cell), actionTree.getRoot());

        assertFalse(branchManager.isInViewMode());

        // Enter view mode
        branchManager.enterViewMode(node1);
        assertTrue(branchManager.isInViewMode());
        assertNotNull(branchManager.getViewContext());

        // Exit view mode
        branchManager.exitViewMode();
        assertFalse(branchManager.isInViewMode());
        assertNull(branchManager.getViewContext());
    }

    @Test
    public void testGenerateBranchName() {
        ActionTreeElement node1 = actionTree.add(factory.createAction(5, cell), actionTree.getRoot());

        String name = branchManager.generateBranchName(node1);
        assertNotNull(name);
        assertTrue(name.startsWith("branch-"));
        assertTrue(name.contains("solve"));
    }

    @Test
    public void testRenameBranch() {
        ActionTreeElement node1 = actionTree.add(factory.createAction(5, cell), actionTree.getRoot());
        ActionBranch newBranch = branchManager.createBranch("old-name", node1, false);

        boolean success = branchManager.renameBranch(newBranch.getId(), "new-name");

        assertTrue(success);
        ActionBranch renamed = branchManager.getBranch(newBranch.getId());
        assertEquals("new-name", renamed.getName());
    }
}
