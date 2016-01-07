/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
// Author: Thorsten Jacobi

package org.opalj.bdl.ui.outline

import org.eclipse.xtext.ui.editor.outline.impl.DocumentRootNode
import org.eclipse.emf.ecore.EObject
import org.opalj.bdl.bDL.BDLPackage
import org.eclipse.xtext.ui.editor.outline.IOutlineNode
import org.eclipse.xtext.ui.editor.outline.impl.EStructuralFeatureNode
import org.opalj.bdl.bDL.IssueElement
import java.util.HashSet
import java.util.HashMap
import org.opalj.bdl.bDL.IssueCategoryElement
import org.eclipse.emf.ecore.EStructuralFeature
import org.opalj.bdl.bDL.IssueClassElement
import org.eclipse.xtext.util.ITextRegion
import org.eclipse.swt.graphics.Image
import org.opalj.bdl.bDL.IssueMethodElement

/**
 * Customization of the default outline structure.
 *
 * See https://www.eclipse.org/Xtext/documentation/304_ide_concepts.html#outline
 */
class BDLOutlineTreeProvider extends org.eclipse.xtext.ui.editor.outline.impl.DefaultOutlineTreeProvider {
	
	// to add/remove filters edit the following things:
	//	1) add/remove the static key
	//	2) add/remove to/from KEYS_FILTERBY
	//	3) add/remove in getPossibleOptionsForFilter
	//	4) add/remove in getFilteredIssues
	
	public static String KEY_FILTERBY_TYPE 			= 'filter by type';
	public static String KEY_FILTERBY_RELEVANCE		= 'filter by relevance';
	public static String KEY_FILTERBY_CATEGORY		= 'filter by category';
	public static String KEY_FILTERBY_KINDS			= 'filter by kind';
	public static String KEY_FILTERBY_PACKAGES		= 'filter by package';
	public static String KEY_FILTERBY_CLASS			= 'filter by class';
	
	public static String[] KEYS_FILTERBY = #[KEY_FILTERBY_TYPE, KEY_FILTERBY_RELEVANCE, KEY_FILTERBY_CATEGORY, KEY_FILTERBY_KINDS, KEY_FILTERBY_PACKAGES, KEY_FILTERBY_CLASS]
	
	// generates the root structure
	override protected _createChildren(DocumentRootNode parentNode, EObject modelElement) {
		// show the default document structure
		createNode(parentNode, modelElement);
		// show filters
		showPossibleFilters(parentNode, modelElement, getPossibleFilters(null, getFilteredIssues(modelElement, null )));
	}
	
	override createChildren(IOutlineNode parent, EObject modelElement) {

		if (modelElement instanceof IssueElement){
			createIssueElementNode(parent, modelElement);
			return;
		}
		
		if (!(parent instanceof EStructuralFeatureNode)){
			super.createChildren(parent, modelElement);
			return;
		}
		
		var appliedFilters = getAppliedFilters(parent);
		var HashSet<IssueElement> filteredIssues = getFilteredIssues(modelElement, appliedFilters);

		if (KEYS_FILTERBY.contains(parent.text)){ // a filter is selected, show possible parameters
			showPossibleOptionsForFilter(parent, modelElement, parent.text as String, filteredIssues);
		}else{ // some filter was selected, show entries
			var HashSet<String> possible = getPossibleFilters(appliedFilters, filteredIssues);
			showPossibleFilters(parent, modelElement, possible);
			
			// show filtered elements
			for (IssueElement issue : filteredIssues)
				createNode(parent , issue);
		}

	}
	
	// special handling of Package/Class element
	// -> either have a package or a class element in the outline
	def createIssueElementNode(IOutlineNode parent, IssueElement element){
		for (EStructuralFeature feature : BDLPackage.Literals.ISSUE_ELEMENT.EAllStructuralFeatures){
			if (feature.featureID == BDLPackage.ISSUE_ELEMENT__CLASS){
				if (
					(element.class_ != null) 
				){
					var IssueClassElement cls = element.class_;
					var String text = _text(cls).toString;
					if (element.package != null)
						text = _text(element.package) + text;
					
					createEStructuralFeatureNode(parent, element, BDLPackage.Literals.ISSUE_ELEMENT__CLASS, _image(cls), text, true);
				}
			}else if (
					(feature.featureID == BDLPackage.ISSUE_ELEMENT__PACKAGE) &&
					(element.eGet(BDLPackage.Literals.ISSUE_ELEMENT__CLASS) != null)
					)
			{
				
			}else{
				if (
					(element.eGet(feature) != null) && 
					(element.eGet(feature) instanceof EObject)
				)
					createNode(parent, element.eGet(feature) as EObject);
			}
		}
	}
	
	def getPossibleFilters(HashMap<String,String> used, HashSet<IssueElement> currentIssues){
		if (currentIssues.length <= 1) return new HashSet<String>(); // makes no sense to filter ...
		var possible = new HashSet<String>(KEYS_FILTERBY);

		// remove filters which are already used
		if (used != null)
			for (String sKey : used.keySet)
				possible.remove(sKey);

		// remove filters which have only one possible selection
		for (var i = possible.length-1; i >= 0; i--){
			if (getPossibleOptionsForFilter( possible.get(i), currentIssues).length <= 1)
				possible.remove(possible.get(i));
		}
		
		return possible;
	}
	def getAppliedFilters(IOutlineNode parent){
		var ret = new HashMap<String,String>();
		
		var IOutlineNode previous = null;
		var current = parent;
		while (current != null){
			if ((current instanceof EStructuralFeatureNode) && (previous != null)){
				if (KEYS_FILTERBY.contains(current.text))
					ret.put(current.text as String, previous.text as String);
			}
			previous = current;
			current = current.parent;
		}
		
		return ret;
	}
	
	def showPossibleFilters(IOutlineNode parent, EObject modelElement, HashSet<String> possible){
		for (String sFilter : possible){
			createEStructuralFeatureNodeOwn(parent, modelElement, BDLPackage.Literals.MODEL_CONTAINER__NAME, _image(sFilter), sFilter, false);
		}
	}

	def showPossibleOptionsForFilter(IOutlineNode parent, EObject modelElement, String filter, HashSet<IssueElement> currentIssues){

		for (Object ele : getPossibleOptionsForFilter(filter, currentIssues))
			createEStructuralFeatureNodeOwn(parent, modelElement, BDLPackage.Literals.MODEL_CONTAINER__NAME, null, ele.toString, false);

	}

	def HashSet<Object> getPossibleOptionsForFilter(String filter, HashSet<IssueElement> currentIssues){
		var possible = new HashSet();

		for (IssueElement issue: currentIssues)
			if (filter.equals(KEY_FILTERBY_TYPE)){
				for (var i = 0; i < issue.name.length; i++)
					if (!possible.contains(issue.name.get(i))){
						possible.add(issue.name.get(i));
					}
			}else if (filter.equals(KEY_FILTERBY_RELEVANCE)){
				if ( (issue.relevance != null) && (!possible.contains(issue.relevance.relevance)) ){
					possible.add(issue.relevance.relevance);
				}
			}else if (filter.equals(KEY_FILTERBY_CATEGORY)){
				if (issue.categories != null)
					for (String cat : issue.categories.elements){
						if (!possible.contains(cat)){
							possible.add(cat);
						}	
					}
			}else if (filter.equals(KEY_FILTERBY_KINDS)){
				if (issue.kinds != null)
					for (String kind: issue.kinds.elements){
						if (!possible.contains(kind)){
							possible.add(kind);
						}	
					}
			}else if (filter.equals(KEY_FILTERBY_PACKAGES)){
				if ( (issue.package != null) && (!possible.contains(issue.package.package))){
					possible.add(issue.package.package);
				}
			}else if (filter.equals(KEY_FILTERBY_CLASS)){
				if ( (issue.class_ != null) && (!possible.contains(issue.class_.class_)) ){
					possible.add(issue.class_.class_);
				}
			}

		return possible;
	}

	def getFilteredIssues(EObject modelElement, HashMap<String,String> appliedFilters){
		var ret = new HashSet<IssueElement>();
		
		var iter = modelElement.eAllContents;
		while (iter.hasNext){
			var current = iter.next;
			if (current.eClass.name.equals('IssueElement')){
				var issue = current as IssueElement;
				
				var boolean isOK = true;
				if (appliedFilters != null)
					for (String filter: appliedFilters.keySet){
						var boolean thisFilter = false;
						if (filter.equals(KEY_FILTERBY_TYPE)){
							for (var i = 0; i < issue.name.length; i++){
								if (issue.name.get(i).equals(appliedFilters.get(filter)))
									thisFilter = true;
							}
						}else if (filter.equals(KEY_FILTERBY_RELEVANCE)){
							if (issue.relevance.relevance.toString.equals(appliedFilters.get(filter)))
								thisFilter = true;
						}else if (filter.equals(KEY_FILTERBY_CATEGORY)){
							for (var i = 0; i < issue.categories.elements.length; i++){
								if (issue.categories.elements.get(i).equals(appliedFilters.get(filter)))
									thisFilter = true;
							}
						}else if (filter.equals(KEY_FILTERBY_KINDS)){
							for (var i = 0; i < issue.kinds.elements.length; i++){
								if (issue.kinds.elements.get(i).equals(appliedFilters.get(filter)))
									thisFilter = true;
							}
						}else if (filter.equals(KEY_FILTERBY_PACKAGES)){
							if (issue.package.package.toString.equals(appliedFilters.get(filter)))
								thisFilter = true;
						}else if (filter.equals(KEY_FILTERBY_CLASS)){
							if (issue.class_.class_.toString.equals(appliedFilters.get(filter)))
								thisFilter = true;
						}
	
						isOK = isOK && thisFilter;
					}
			
				if (isOK)
					ret.add(issue);
			}
		}
		
		return ret;
	}
	
	
	// override for the category element ... troublemaker!
	override protected _isLeaf(EObject modelElement) {
		if (modelElement instanceof IssueCategoryElement) return true;
		if (modelElement instanceof IssueMethodElement) return true;
		super._isLeaf(modelElement)
	}
	
	/*
	 * this function is nearly identical with the original one
	 * BUT it removes the text region for the element -> set to empty
	 * this SHOULD fix the long loading time of documents in some packages
	 * where the editor searches for the "best" outline node for a given text
	 * @see org.eclipse.xtext.ui.editor.outline.actions.OutlineWithEditorLinker.findBestNode(IOutlineNode, ITextRegion)
	 * (that function iterates over all children in the outline effectively testing all filter options ...)
	 */
	def EStructuralFeatureNode createEStructuralFeatureNodeOwn(IOutlineNode parentNode, EObject owner,
			EStructuralFeature feature, Image image, Object text, boolean isLeaf) {
		var boolean isFeatureSet = owner.eIsSet(feature);
		var EStructuralFeatureNode eStructuralFeatureNode = new EStructuralFeatureNode(owner, feature, parentNode, image,
				text, isLeaf || !isFeatureSet);
		if (isFeatureSet) {
			var ITextRegion region = ITextRegion.EMPTY_REGION;
			eStructuralFeatureNode.setTextRegion(region);
		}
		return eStructuralFeatureNode;
	}	
}