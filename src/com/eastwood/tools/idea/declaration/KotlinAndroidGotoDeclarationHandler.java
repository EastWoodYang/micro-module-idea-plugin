package com.eastwood.tools.idea.declaration;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.util.AndroidResourceUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.android.navigation.GotoResourceHelperKt;
import org.jetbrains.kotlin.psi.KtSimpleNameExpression;

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

        return info == null ? null : GotoResourceHelper.getGotoDeclarationTargets(facet, info, referenceExpression);
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        return
                null;
    }

}