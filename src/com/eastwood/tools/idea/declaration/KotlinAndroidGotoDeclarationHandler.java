package com.eastwood.tools.idea.declaration;

import com.android.ide.common.rendering.api.ResourceNamespace;
import com.android.resources.ResourceType;
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import org.jetbrains.android.dom.resources.Attr;
import org.jetbrains.android.dom.resources.DeclareStyleable;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.resourceManagers.LocalResourceManager;
import org.jetbrains.android.resourceManagers.ModuleResourceManagers;
import org.jetbrains.android.resourceManagers.ResourceManager;
import org.jetbrains.android.util.AndroidResourceUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.android.navigation.GotoResourceHelperKt;
import org.jetbrains.kotlin.psi.KtSimpleNameExpression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KotlinAndroidGotoDeclarationHandler implements GotoDeclarationHandler {

    @Override
    public PsiElement[] getGotoDeclarationTargets(@Nullable PsiElement sourceElement, int offset, Editor editor) {
        KtSimpleNameExpression referenceExpression = GotoResourceHelperKt.getReferenceExpression(sourceElement);
        if (referenceExpression == null) {
            return null;
        }

        AndroidFacet facet = AndroidFacet.getInstance(referenceExpression);
        if (facet == null) {
            return null;
        }

        AndroidResourceUtil.MyReferredResourceFieldInfo info = GotoResourceHelper.getReferredResource(facet, referenceExpression);

        if (info == null) return null;

        String nestedClassName = info.getClassName();
        String fieldName = info.getFieldName();
        List<PsiElement> resourceList = new ArrayList<PsiElement>();
        if (info.isFromManifest()) {
            return null;
        }

        ModuleResourceManagers managers = ModuleResourceManagers.getInstance(facet);
        ResourceManager manager = info.getNamespace() == ResourceNamespace.ANDROID ? managers.getFrameworkResourceManager(false) : managers.getLocalResourceManager();
        if (manager == null) {
            return null;
        }

        manager.collectLazyResourceElements(info.getNamespace(), nestedClassName, fieldName, false, referenceExpression, resourceList);

        if (manager instanceof LocalResourceManager) {
            LocalResourceManager lrm = (LocalResourceManager) manager;

            if (nestedClassName.equals(ResourceType.ATTR.getName())) {
                for (Attr attr : lrm.findAttrs(info.getNamespace(), fieldName)) {
                    resourceList.add(attr.getName().getXmlAttributeValue());
                }
            } else if (nestedClassName.equals(ResourceType.STYLEABLE.getName())) {
                for (DeclareStyleable styleable : lrm.findStyleables(info.getNamespace(), fieldName)) {
                    resourceList.add(styleable.getName().getXmlAttributeValue());
                }

                for (Attr styleable : lrm.findStyleableAttributesByFieldName(info.getNamespace(), fieldName)) {
                    resourceList.add(styleable.getName().getXmlAttributeValue());
                }
            }
        }

        if (resourceList.size() > 1) {
            // Sort to ensure the output is stable, and to prefer the base folders
            Collections.sort(resourceList, AndroidResourceUtil.RESOURCE_ELEMENT_COMPARATOR);
        }

        return resourceList.toArray(new PsiElement[resourceList.size()]);
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        return null;
    }

}