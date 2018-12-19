package com.eastwood.tools.idea;

import com.android.ide.common.rendering.api.ResourceNamespace;
import com.android.tools.idea.res.psi.AndroidResourceToPsiResolver;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.util.AndroidResourceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.jetbrains.android.util.AndroidResourceUtil.*;

public class AndroidGotoDeclarationHandler extends org.jetbrains.android.AndroidGotoDeclarationHandler {

    public static AndroidResourceUtil.MyReferredResourceFieldInfo getReferredResourceOrManifestField(@NotNull AndroidFacet facet, @NotNull PsiReferenceExpression exp, @Nullable String className, boolean localOnly) {
        String resFieldName = exp.getReferenceName();
        if (resFieldName != null && !resFieldName.isEmpty()) {
            PsiExpression qExp = exp.getQualifierExpression();
            if (!(qExp instanceof PsiReferenceExpression)) {
                return null;
            } else {
                PsiReferenceExpression resClassReference = (PsiReferenceExpression) qExp;
                String resClassName = resClassReference.getReferenceName();
                if (resClassName != null && !resClassName.isEmpty() && (className == null || className.equals(resClassName))) {
                    qExp = resClassReference.getQualifierExpression();
                    if (!(qExp instanceof PsiReferenceExpression)) {
                        return null;
                    } else {
                        PsiElement resolvedElement = ((PsiReferenceExpression) qExp).resolve();
                        if (!(resolvedElement instanceof PsiClass)) {
                            return null;
                        } else {
                            Module resolvedModule = ModuleUtilCore.findModuleForPsiElement(resolvedElement);
                            PsiClass aClass = (PsiClass) resolvedElement;
                            String classShortName = aClass.getName();
                            boolean fromManifest = "Manifest".equals(classShortName);
                            if (!fromManifest && !"R".equals(classShortName)) {
                                return null;
                            } else {
                                String qName = aClass.getQualifiedName();
                                if (qName == null) {
                                    return null;
                                } else if (!localOnly && ("android.R".equals(qName) || "com.android.internal.R".equals(qName))) {
                                    return new AndroidResourceUtil.MyReferredResourceFieldInfo(resClassName, resFieldName, resolvedModule, ResourceNamespace.ANDROID, false);
                                } else {
                                    if (fromManifest) {
                                        if (!isManifestClass(aClass)) {
                                            return null;
                                        }
                                    } else if (!isRJavaClass(aClass)) {
                                        if (!isMicroModuleRJavaFile(aClass)) {
                                            return null;
                                        }
                                    }

                                    return new AndroidResourceUtil.MyReferredResourceFieldInfo(resClassName, resFieldName, resolvedModule, getRClassNamespace(facet, qName), fromManifest);
                                }
                            }
                        }
                    }
                } else {
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    @Nullable
    public static AndroidResourceUtil.MyReferredResourceFieldInfo getReferredResourceOrManifestField(@NotNull AndroidFacet facet, @NotNull PsiReferenceExpression exp, boolean localOnly) {
        return getReferredResourceOrManifestField(facet, exp, (String) null, localOnly);
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

    @Override
    public PsiElement[] getGotoDeclarationTargets(@Nullable PsiElement sourceElement, int offset, Editor editor) {
        if (!(sourceElement instanceof PsiIdentifier)) {
            return null;
        } else {
            PsiFile file = sourceElement.getContainingFile();
            if (file == null) {
                return null;
            } else {
                AndroidFacet facet = AndroidFacet.getInstance(file);
                if (facet == null) {
                    return null;
                } else {
                    PsiReferenceExpression refExp = (PsiReferenceExpression) PsiTreeUtil.getParentOfType(sourceElement, PsiReferenceExpression.class);
                    if (refExp == null) {
                        return null;
                    } else {
                        AndroidResourceUtil.MyReferredResourceFieldInfo info = getReferredResourceOrManifestField(facet, refExp, false);
                        if (info == null) {
                            PsiElement parent = refExp.getParent();
                            if (parent instanceof PsiReferenceExpression) {
                                info = getReferredResourceOrManifestField(facet, (PsiReferenceExpression) parent, false);
                            }

                            if (info == null) {
                                parent = parent.getParent();
                                if (parent instanceof PsiReferenceExpression) {
                                    info = getReferredResourceOrManifestField(facet, (PsiReferenceExpression) parent, false);
                                }
                            }
                        }

                        return info == null ? null : AndroidResourceToPsiResolver.getInstance().getGotoDeclarationTargets(info, refExp);
                    }
                }
            }
        }
    }

}
