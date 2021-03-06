package org.smultron.framework.content.dialog;

import org.rspeer.runetek.adapter.scene.Npc;
import org.rspeer.runetek.api.commons.Time;
import org.rspeer.runetek.api.component.Dialog;
import org.rspeer.ui.Log;
import org.smultron.framework.content.InteractWith;
import org.smultron.framework.tasks.FunctionalTask;
import org.smultron.framework.tasks.Task;
import org.smultron.framework.thegreatforest.BinaryBranch;
import org.smultron.framework.thegreatforest.LeafNode;
import org.smultron.framework.thegreatforest.TreeNode;

import java.util.Deque;
import java.util.function.Supplier;

/**
 * Processes dialog options and continues
 * Validates when all options has been processed and the dialog is no longer open.
 */
public class ProcessDialogTree extends BinaryBranch {
	private String[] options;
	private int optionIndex = 0;
	private Supplier<Npc> npc;
	private String currentOption;


	/**
	 * @param options the options which it will process.
	 *                The task will get stck if an option does not exist or the {@link Deque<String>} is out of order
	 * @param npc     tries to Talk-to with this npc if the dialoge is not open
	 */
	public ProcessDialogTree(Supplier<Npc> npc, final String... options) {
		super();
		this.options = options.clone();
		this.npc = npc;
		currentOption = options[0];
		setSuccessNode(this::processDialog);
		setValidation(this::validate);
		setFailureNode(this::startDialog);
	}

	/**
	 * Executes when not in dialog
	 */
	private TreeNode startDialog() {
		return new LeafNode(new InteractWith<>("Talk-to", npc));
	}

	/**
	 * Executes when in dialog
	 */
	private TreeNode processDialog() {
		if (Dialog.canContinue())
			return new LeafNode(ProcessContinue.processContinue());
		else
			return new LeafNode(processOption());
	}

	private Task processOption() {
		Task processOption = new FunctionalTask(() -> {
			if (Dialog.getChatOption(s -> s.equals(currentOption)) != null) {
				if (Dialog.process(s -> s.equals(currentOption))) {
					Time.sleepUntil(() -> !Dialog.isProcessing(), 2000);
					currentOption = getNextOption();
				} else {
					Log.severe("Could not click on " + currentOption);
				}
			} else {
				Log.severe("Option " + currentOption + " does not exist.");
			}
		});
		processOption.setName("I want to say " + currentOption);
		return processOption;
	}

	private String getNextOption() {
		if (optionIndex <= options.length) {
			optionIndex++;
			return options[optionIndex];
		}
		return "No options left.";
	}

	private boolean validate() {
		Time.sleepUntil(() -> !Dialog.isProcessing(), 1000);
		return Dialog.isOpen() && optionIndex <= options.length;
	}
}
