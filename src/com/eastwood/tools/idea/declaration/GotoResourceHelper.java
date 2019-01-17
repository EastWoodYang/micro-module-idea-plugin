package com.eastwood.tools.idea.declaration;

import com.android.resources.ResourceType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.*;
import org.jetbrains.android.dom.resources.Attr;
import org.jetbrains.android.dom.resources.DeclareStyleable;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.resourceManagers.LocalResourceManager;
import org.jetbrains.android.resourceManagers.ModuleResourceManagers;
import org.jetbrains.android.resourceManagers.ResourceManager;
import org.jetbrains.android.util.AndroidResourceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.references.ReferenceUtilKt;
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtSimpleNameExpression;
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;

import java.util.ArrayList;
import java.util.List;

public class GotoResourceHelper {

    public static AndroidResourceUtil.MyReferredResourceFieldInfo getReferredResource(AndroidFacet facet, PsiReferenceExpression exp) {
        String resFieldName = exp.getReferenceName();
        if (resFieldName == null || resFieldName.isEmpty()) {
            return null;
        }

        PsiExpression qExp = exp.getQualifierExpression();
        if (!(qExp instanceof PsiReferenceExpression)) {
            return null;
        }

        PsiReferenceExpression resClassReference = (PsiReferenceExpression) qExp;
        String resClassName = resClassReference.getReferenceName();
        if (resClassName == null || resClassName.isEmpty()) {
            return null;
        }

        qExp = resClassReference.getQualifierExpression();
        if (!(qExp instanceof PsiReferenceExpression)) {
            return null;
        }

        PsiElement resolvedElement = ((PsiReferenceExpression) qExp).resolve();
        if (!(resolvedElement instanceof PsiClass)) {
            return null;
        }

        return getReferredResource(facet, resClassName, resFieldName, resolvedElement);
    }

    public static AndroidResourceUtil.MyReferredResourceFieldInfo getReferredResource(AndroidFacet facet, KtSimpleNameExpression referenceExpression) {
        String resFieldName = referenceExpression.getReferencedName();
        if (resFieldName.length() == 0) return null;

        KtSimpleNameExpression middlePart = getReceiverAsSimpleNameExpression(referenceExpression);
        if (middlePart == null) return null;


        String resClassName = middlePart.getReferencedName();
        if (resClassName.length() == 0) {
            return null;
        }

        KtSimpleNameExpression firstPart = getReceiverAsSimpleNameExpression(middlePart);
        if (firstPart == null) return null;

        PsiElement resolvedElement = ReferenceUtilKt.getMainReference(firstPart).resolve();
        if (!(resolvedElement instanceof PsiClass)) {
            resolvedElement = null;
        }

        if (resolvedElement == null) return null;

        return getReferredResource(facet, resClassName, resFieldName, resolvedElement);
    }

    private static KtSimpleNameExpression getReceiverAsSimpleNameExpression(KtSimpleNameExpression exp) {
        KtExpression receiver = KtPsiUtilKt.getReceiverExpression(exp);
        KtSimpleNameExpression simpleNameExpression;
        if (receiver instanceof KtSimpleNameExpression) {
            simpleNameExpression = (KtSimpleNameExpression) receiver;
        } else if (receiver instanceof KtDotQualifiedExpression) {
            KtExpression expression = ((KtDotQualifiedExpression) receiver).getSelectorExpression();
            if (!(expression instanceof KtSimpleNameExpression)) {
                expression = null;
            }

            simpleNameExpression = (KtSimpleNameExpression) expression;
        } else {
            simpleNameExpression = null;
        }

        return simpleNameExpression;
    }

    private static AndroidResourceUtil.MyReferredResourceFieldInfo getReferredResource(AndroidFacet facet, String resClassName, String resFieldName, PsiElement resolvedElement) {
        Module resolvedModule = ModuleUtilCore.findModuleForPsiElement(resolvedElement);

        PsiClass resolvedClass = (PsiClass) resolvedElement;
        String classShortName = resolvedClass.getName();
        boolean fromManifest = "Manifest".equals(classShortName);
        if (fromManifest || !"R".equals(classShortName)) {
            return null;
        }

        String qName = resolvedClass.getQualifiedName();
        if (qName == null) {
            return null;
        } else if (("android.R".equals(qName) || "com.android.internal.R".equals(qName))) {
            return new AndroidResourceUtil.MyReferredResourceFieldInfo(resClassName, resFieldName, resolvedModule, true, false);
        } else {
            PsiFile containingFile = resolvedClass.getContainingFile();
            if (containingFile == null) return null;

            if (!AndroidResourceUtil.isRJavaFile(facet, containingFile)) {
                if (!isMicroModuleRJavaFile(resolvedClass)) {
                    return null;
                }
            }

            return new AndroidResourceUtil.MyReferredResourceFieldInfo(resClassName, resFieldName, resolvedModule, false, fromManifest);
        }
    }

    private static boolean isMicroModuleRJavaFile(@NotNull PsiClass psiClass) {
        PsiFile file = psiClass.getContainingFile();
        if (file.getName().equals("R.java")) {
            if (file.getVirtualFile().getPath().contains("build")) {
                return true;
            }
        }
        return false;
    }

    public static PsiElement[] getGotoDeclarationTargets(AndroidFacet facet, AndroidResourceUtil.MyReferredResourceFieldInfo info, PsiElement refExp) {
        String nestedClassName = info.getClassName();
        String fieldName = info.getFieldName();
        List<PsiElement> resourceList = new ArrayList();
        if (info.isFromManifest()) {
            return null;
        }

        ModuleResourceManagers resourceManagers = ModuleResourceManagers.getInstance(facet);
        ResourceManager manager = info.isSystem() ? resourceManagers.getSystemResourceManager(false) : resourceManagers.getLocalResourceManager();
        if (manager == null) {
            return null;
        }

        manager.collectLazyResourceElements(nestedClassName, fieldName, false, refExp, resourceList);

        if (manager instanceof LocalResourceManager) {
            LocalResourceManager lrm = (LocalResourceManager) manager;
            if (nestedClassName.equals(ResourceType.ATTR.getName())) {
                for (Attr attr : lrm.findAttrs(fieldName)) {
                    resourceList.add(attr.getName().getXmlAttributeValue());
                }
            } else if (nestedClassName.equals(ResourceType.STYLEABLE.getName())) {
                for (DeclareStyleable styleable : lrm.findStyleables(fieldName)) {
                    resourceList.add(styleable.getName().getXmlAttributeValue());
                }

                for (Attr styleable : lrm.findStyleableAttributesByFieldName(fieldName)) {
                    resourceList.add(styleable.getName().getXmlAttributeValue());
                }
            }
        }

        if (resourceList.size() > 1) {
            resourceList.sort(AndroidResourceUtil.RESOURCE_ELEMENT_COMPARATOR);
        }

        return resourceList.toArray(new PsiElement[resourceList.size()]);
    }

}
